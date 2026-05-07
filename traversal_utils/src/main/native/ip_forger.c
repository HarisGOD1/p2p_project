/**
 * IP Header Forge and Post Service
 * * A Linux-based command-line utility for Kotlin P2P IPC integration.
 * Builds and injects raw IPv4 headers using <linux/ip.h>.
 * * Compilation:
 * gcc -o ip_forger ip_forger.c
 * * Capability Assignment (Required for --send):
 * sudo setcap cap_net_raw+ep ./ip_forger
 * * Usage Examples:
 * JSON Simulation: ./ip_forger --src 192.168.1.10 --dst 10.0.0.5 --ttl 128
 * Network Injection: ./ip_forger -s 192.168.1.10 -d 10.0.0.5 -x
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <getopt.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <linux/ip.h>
#include <errno.h>

#define MAX_PACKET_SIZE 65535

// Lightweight Base64 Decoder
size_t base64_decode(const char *in, unsigned char *out) {
    int b64_table[256];
    memset(b64_table, -1, sizeof(b64_table));
    for (int i = 0; i < 26; i++) { b64_table['A' + i] = i; b64_table['a' + i] = i + 26; }
    for (int i = 0; i < 10; i++) { b64_table['0' + i] = i + 52; }
    b64_table['+'] = 62; b64_table['/'] = 63;
    
    size_t len = strlen(in), out_len = 0;
    int val = 0, valb = -8;
    for (size_t i = 0; i < len; i++) {
        unsigned char c = in[i];
        if (b64_table[c] != -1) {
            val = (val << 6) | b64_table[c];
            valb += 6;
            if (valb >= 0) {
                out[out_len++] = (val >> valb) & 0xFF;
                valb -= 8;
            }
        }
    }
    return out_len;
}

// Mathematical Integrity: IPv4 Checksum Algorithm (One's Complement)
unsigned short calculate_ip_checksum(struct iphdr *iph) {
    // Temporarily set the checksum field to 0 for the calculation
    iph->check = 0;
    
    unsigned short *addr = (unsigned short *)iph;
    // Calculate length in bytes (ihl is in 32-bit words, so multiply by 4)
    unsigned int count = iph->ihl << 2; 
    register unsigned long sum = 0;

    // Accumulate the 16-bit words
    while (count > 1) {
        sum += *addr++;
        count -= 2;
    }

    // Handle odd-byte padding if header length is oddly configured
    if (count > 0) {
        sum += ((*addr) & htons(0xFF00));
    }

    // Fold the 32-bit sum into 16 bits
    while (sum >> 16) {
        sum = (sum & 0xFFFF) + (sum >> 16);
    }

    // Return the one's complement cast to a 16-bit unsigned short
    return (unsigned short)(~sum);
}

void print_usage(const char* prog_name) {
    fprintf(stderr, "Usage: %s -s <src_ip> -d <dst_ip> [options]\n", prog_name);
    fprintf(stderr, "Options:\n");
    fprintf(stderr, "  -s, --src     Source IP Address (Mandatory)\n");
    fprintf(stderr, "  -d, --dst     Destination IP Address (Mandatory)\n");
    fprintf(stderr, "  -p, --proto   Protocol (Default: 255)\n");
    fprintf(stderr, "  -t, --ttl     Time to Live (Default: 64)\n");
    fprintf(stderr, "  -i, --id      Identification (Default: 54321)\n");
    fprintf(stderr, "  -o, --tos     Type of Service (Default: 0)\n");
    fprintf(stderr, "  -f, --frag    Fragment Offset (Default: 0)\n");
    fprintf(stderr, "  -b, --data    Base64 Encoded Payload Data\n");
    fprintf(stderr, "  -x, --send    Send over network (If omitted, outputs JSON)\n");
}

int main(int argc, char *argv[]) {
    // --- 1. Default Variables and State Initialization ---
    char *src_ip_str = NULL;
    char *dst_ip_str = NULL;
    char *b64_payload = NULL;
    uint8_t proto = 255;
    uint8_t ttl = 64;
    uint16_t id = 54321;
    uint8_t tos = 0;
    uint16_t frag_off = 0;
    int send_flag = 0; // 0 = JSON mode, 1 = Raw Socket injection

    // --- 2. Command-Line Argument Orchestration ---
    static struct option long_options[] = {
        {"src",   required_argument, 0, 's'},
        {"dst",   required_argument, 0, 'd'},
        {"proto", required_argument, 0, 'p'},
        {"ttl",   required_argument, 0, 't'},
        {"id",    required_argument, 0, 'i'},
        {"tos",   required_argument, 0, 'o'},
        {"frag",  required_argument, 0, 'f'},
        {"data",  required_argument, 0, 'b'},
        {"send",  no_argument,       0, 'x'},
        {0, 0, 0, 0}
    };

    int opt;
    while ((opt = getopt_long(argc, argv, "s:d:p:t:i:o:f:b:x", long_options, NULL)) != -1) {
        switch (opt) {
            case 's': src_ip_str = optarg; break;
            case 'd': dst_ip_str = optarg; break;
            case 'p': proto = (uint8_t)atoi(optarg); break;
            case 't': ttl = (uint8_t)atoi(optarg); break;
            case 'i': id = (uint16_t)atoi(optarg); break;
            case 'o': tos = (uint8_t)atoi(optarg); break;
            case 'f': frag_off = (uint16_t)atoi(optarg); break;
            case 'b': b64_payload = optarg; break;
            case 'x': send_flag = 1; break;
            default: print_usage(argv[0]); exit(EXIT_FAILURE);
        }
    }

    // Validation: Source and Destination IPs are mandatory
    if (!src_ip_str || !dst_ip_str) {
        fprintf(stderr, "[!] Error: Both --src and --dst arguments are required.\n");
        print_usage(argv[0]);
        exit(EXIT_FAILURE);
    }

    // --- 3. IP Header Construction ---
    // Allocate buffer for header and potential payload (max IP packet size)
    char packet[MAX_PACKET_SIZE];
    memset(packet, 0, sizeof(packet));

    // Map the struct iphdr directly to the buffer's memory space
    struct iphdr *iph = (struct iphdr *)packet;

    // Handle payload (Base64 Decoding)
    size_t payload_len = 0;
    unsigned char *payload_ptr = (unsigned char *)(packet + sizeof(struct iphdr));
    
    if (b64_payload != NULL) {
        payload_len = base64_decode(b64_payload, payload_ptr);
        
        if (sizeof(struct iphdr) + payload_len > MAX_PACKET_SIZE) {
            fprintf(stderr, "[!] Payload too large for standard IPv4 packet.\n");
            exit(EXIT_FAILURE);
        }
    }

    // Populate fixed and configured parameters
    iph->version = 4;
    iph->ihl = 5; // 5 * 4 = 20 bytes (standard header size)
    iph->tos = tos;
    iph->tot_len = htons(sizeof(struct iphdr) + payload_len); // Include payload length
    iph->id = htons(id);
    iph->frag_off = htons(frag_off);
    iph->ttl = ttl;
    iph->protocol = proto;

    // Translate IP strings to network-byte-order integers
    if (inet_pton(AF_INET, src_ip_str, &(iph->saddr)) != 1) {
        fprintf(stderr, "[!] Invalid Source IP format.\n");
        exit(EXIT_FAILURE);
    }
    
    if (inet_pton(AF_INET, dst_ip_str, &(iph->daddr)) != 1) {
        fprintf(stderr, "[!] Invalid Destination IP format.\n");
        exit(EXIT_FAILURE);
    }

    // Calculate integrity checksum
    iph->check = calculate_ip_checksum(iph);

    // --- 4. Execution Pipeline Branching ---
    if (!send_flag) {
        // [BRANCH A] SIMULATION MODE: Native JSON Serialization
        
        // Convert internal representations back to readable strings
        char s_str[INET_ADDRSTRLEN];
        char d_str[INET_ADDRSTRLEN];
        inet_ntop(AF_INET, &(iph->saddr), s_str, INET_ADDRSTRLEN);
        inet_ntop(AF_INET, &(iph->daddr), d_str, INET_ADDRSTRLEN);

        // Emit strictly formatted JSON
        printf("{\n");
        printf("  \"Forged\": true,\n");
        printf("  \"header\": {\n");
        printf("    \"version\": %u,\n", iph->version);
        printf("    \"ihl\": %u,\n", iph->ihl);
        printf("    \"tos\": %u,\n", iph->tos);
        printf("    \"tot_len\": %u,\n", ntohs(iph->tot_len));
        printf("    \"id\": %u,\n", ntohs(iph->id));
        printf("    \"frag_off\": %u,\n", ntohs(iph->frag_off));
        printf("    \"ttl\": %u,\n", iph->ttl);
        printf("    \"protocol\": %u,\n", iph->protocol);
        printf("    \"check\": %u,\n", ntohs(iph->check));
        printf("    \"saddr\": \"%s\",\n", s_str);
        printf("    \"daddr\": \"%s\"\n", d_str);
        printf("  },\n");
        printf("  \"payload_len\": %zu,\n", payload_len);
        if (b64_payload) {
            printf("  \"payload_b64\": \"%s\"\n", b64_payload);
        } else {
            printf("  \"payload_b64\": null\n");
        }
        printf("}\n");
        
        // Ensure stream is flushed immediately for IPC piping to Kotlin
        fflush(stdout); 

    } else {
        // [BRANCH B] ACTIVE MODE: Network Injection via Raw Sockets
        
        // IPPROTO_RAW implicitly enables the IP_HDRINCL socket option in Linux
        int sockfd = socket(AF_INET, SOCK_RAW, IPPROTO_RAW);
        if (sockfd < 0) {
            perror("[!] Socket creation failed. Have you set CAP_NET_RAW or are you running as root?");
            exit(EXIT_FAILURE);
        }

        // Even though dst IP is in the header, kernel routing requires a sockaddr_in
        struct sockaddr_in dest_info;
        memset(&dest_info, 0, sizeof(dest_info));
        dest_info.sin_family = AF_INET;
        dest_info.sin_addr.s_addr = iph->daddr;

        // Inject the packet
        ssize_t bytes_sent = sendto(sockfd, packet, ntohs(iph->tot_len), 0,
                                    (struct sockaddr *)&dest_info, sizeof(dest_info));
        
        if (bytes_sent < 0) {
            perror("[!] Packet injection failed");
            close(sockfd);
            exit(EXIT_FAILURE);
        }
        
        printf("[+] Successfully injected %zd bytes into the network.\n", bytes_sent);
        close(sockfd);
    }

    return 0;
}
