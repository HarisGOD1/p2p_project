/**
 * UDP/IP Header Forge Utility (RFC 768)
 * Handles full UDP Checksum (including Pseudo-Header) and Base64 Payloads.
 * * Compilation: gcc -o udp_forger udp_forger.c
 * Usage (JSON): ./udp_forger -s 192.168.1.10 -d 10.0.0.1 -S 4444 -D 53 -b "SGVsbG8="
 * Usage (Send): ./udp_forger -s 192.168.1.10 -d 10.0.0.1 -S 4444 -D 53 -x
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <getopt.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <linux/ip.h>
#include <linux/udp.h>
#include <errno.h>

#define MAX_PACKET_SIZE 65535

// --- 1. Base64 Decoder ---
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

// --- 2. Checksum Utilities ---
struct pseudo_header {
    uint32_t src_addr;
    uint32_t dst_addr;
    uint8_t  zero;
    uint8_t  protocol;
    uint16_t udp_length;
};

// Generic IP Checksum
unsigned short calculate_checksum(unsigned short *ptr, int nbytes) {
    register long sum = 0;
    while (nbytes > 1) {
        sum += *ptr++;
        nbytes -= 2;
    }
    if (nbytes == 1) {
        uint16_t temp = 0;
        *((uint8_t *)&temp) = *(uint8_t *)ptr;
        sum += temp;
    }
    while (sum >> 16) sum = (sum & 0xFFFF) + (sum >> 16);
    return (unsigned short)(~sum);
}

// RFC 768 UDP Checksum (Accounts for Pseudo Header, UDP Header, and Data)
unsigned short calculate_udp_checksum(struct pseudo_header *psh, struct udphdr *udph, unsigned char *payload, size_t payload_len) {
    register unsigned long sum = 0;
    uint16_t *ptr;
    int i;

    // Add Pseudo Header
    ptr = (uint16_t *)psh;
    for (i = 0; i < (int)(sizeof(struct pseudo_header) / 2); i++) sum += ptr[i];

    // Add UDP Header
    ptr = (uint16_t *)udph;
    for (i = 0; i < (int)(sizeof(struct udphdr) / 2); i++) sum += ptr[i];

    // Add Payload (handles odd-byte padding automatically)
    ptr = (uint16_t *)payload;
    int count = payload_len;
    while (count > 1) {
        sum += *ptr++;
        count -= 2;
    }
    if (count > 0) {
        uint16_t temp = 0;
        *((uint8_t *)&temp) = *(uint8_t *)ptr;
        sum += temp;
    }

    while (sum >> 16) sum = (sum & 0xFFFF) + (sum >> 16);
    
    unsigned short ans = (unsigned short)(~sum);
    // RFC 768: If the computed checksum is zero, it is transmitted as all ones (0xFFFF).
    return (ans == 0x0000) ? 0xFFFF : ans;
}

// --- 3. Main Application ---
int main(int argc, char *argv[]) {
    char *src_ip_str = NULL;
    char *dst_ip_str = NULL;
    char *b64_payload = NULL;
    
    // Default UDP configuration
    uint16_t sport = 12345, dport = 53;
    int send_flag = 0;

    static struct option long_options[] = {
        {"src",   required_argument, 0, 's'},
        {"dst",   required_argument, 0, 'd'},
        {"sport", required_argument, 0, 'S'},
        {"dport", required_argument, 0, 'D'},
        {"data",  required_argument, 0, 'b'},
        {"send",  no_argument,       0, 'x'},
        {0, 0, 0, 0}
    };

    int opt;
    while ((opt = getopt_long(argc, argv, "s:d:S:D:b:x", long_options, NULL)) != -1) {
        switch (opt) {
            case 's': src_ip_str = optarg; break;
            case 'd': dst_ip_str = optarg; break;
            case 'S': sport = (uint16_t)atoi(optarg); break;
            case 'D': dport = (uint16_t)atoi(optarg); break;
            case 'b': b64_payload = optarg; break;
            case 'x': send_flag = 1; break;
            default: exit(EXIT_FAILURE);
        }
    }

    if (!src_ip_str || !dst_ip_str) {
        fprintf(stderr, "[!] Both --src and --dst are mandatory for the Pseudo Header.\n");
        exit(EXIT_FAILURE);
    }

    char packet[MAX_PACKET_SIZE];
    memset(packet, 0, sizeof(packet));

    // Map memory sections
    struct iphdr *iph = (struct iphdr *)packet;
    struct udphdr *udph = (struct udphdr *)(packet + sizeof(struct iphdr));
    unsigned char *payload_ptr = (unsigned char *)(packet + sizeof(struct iphdr) + sizeof(struct udphdr));
    size_t payload_len = 0;

    if (b64_payload != NULL) {
        payload_len = base64_decode(b64_payload, payload_ptr);
    }

    uint16_t udp_total_len = sizeof(struct udphdr) + payload_len;

    // --- 4. Build IP Header ---
    iph->version = 4;
    iph->ihl = 5;
    iph->tot_len = htons(sizeof(struct iphdr) + udp_total_len);
    iph->id = htons(54321);
    iph->ttl = 64;
    iph->protocol = IPPROTO_UDP; // Protocol 17
    inet_pton(AF_INET, src_ip_str, &(iph->saddr));
    inet_pton(AF_INET, dst_ip_str, &(iph->daddr));
    iph->check = calculate_checksum((unsigned short *)iph, iph->ihl * 4);

    // --- 5. Build UDP Header ---
    udph->source = htons(sport);
    udph->dest = htons(dport);
    udph->len = htons(udp_total_len);
    udph->check = 0; // Initialize to 0 for calculation

    // Prepare Pseudo Header
    struct pseudo_header psh;
    psh.src_addr = iph->saddr;
    psh.dst_addr = iph->daddr;
    psh.zero = 0;
    psh.protocol = IPPROTO_UDP;
    psh.udp_length = htons(udp_total_len);

    // Calculate UDP Checksum
    udph->check = calculate_udp_checksum(&psh, udph, payload_ptr, payload_len);

    // --- 6. Execution Branching ---
    if (!send_flag) {
        // Output strictly formatted JSON for Kotlin integration
        printf("{\n");
        printf("  \"Forged\": true,\n");
        printf("  \"udp_header\": {\n");
        printf("    \"sport\": %u,\n", ntohs(udph->source));
        printf("    \"dport\": %u,\n", ntohs(udph->dest));
        printf("    \"len\": %u,\n", ntohs(udph->len));
        printf("    \"checksum\": %u\n", ntohs(udph->check));
        printf("  },\n");
        printf("  \"payload_len\": %zu\n", payload_len);
        printf("}\n");
        fflush(stdout);
    } else {
        // IPPROTO_RAW allows spoofing the source IP
        int sockfd = socket(AF_INET, SOCK_RAW, IPPROTO_RAW);
        if (sockfd < 0) {
            perror("[!] Socket creation failed. Run with CAP_NET_RAW or as root");
            exit(EXIT_FAILURE);
        }

        struct sockaddr_in dest_info;
        memset(&dest_info, 0, sizeof(dest_info));
        dest_info.sin_family = AF_INET;
        dest_info.sin_addr.s_addr = iph->daddr;

        ssize_t bytes_sent = sendto(sockfd, packet, ntohs(iph->tot_len), 0,
                                    (struct sockaddr *)&dest_info, sizeof(dest_info));
        
        if (bytes_sent < 0) {
            perror("[!] Packet injection failed");
        } else {
            printf("[+] Successfully injected %zd bytes.\n", bytes_sent);
        }
        close(sockfd);
    }

    return 0;
}
