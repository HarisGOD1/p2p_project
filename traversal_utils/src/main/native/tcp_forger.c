/**
 * TCP/IP Header Forge Utility
 * Handles full TCP Checksum (including Pseudo-Header), Handshake Flags, and Base64 Payloads.
 * * Compilation: gcc -o tcp_forger tcp_forger.c
 * Usage (JSON): ./tcp_forger -s 192.168.1.10 -d 10.0.0.1 -S 4444 -D 80 -Y -b "SGVsbG8="
 * Usage (Send): ./tcp_forger -s 192.168.1.10 -d 10.0.0.1 -S 4444 -D 80 -Y -x
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <getopt.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <linux/ip.h>
#include <linux/tcp.h>
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
    uint16_t tcp_length;
};

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

// RFC 793 TCP Checksum (Accounts for Pseudo Header, TCP Header, and Data)
unsigned short calculate_tcp_checksum(struct pseudo_header *psh, struct tcphdr *tcph, unsigned char *payload, size_t payload_len) {
    register unsigned long sum = 0;
    uint16_t *ptr;
    int i;

    // Add Pseudo Header
    ptr = (uint16_t *)psh;
    for (i = 0; i < (int)(sizeof(struct pseudo_header) / 2); i++) sum += ptr[i];

    // Add TCP Header
    ptr = (uint16_t *)tcph;
    for (i = 0; i < (int)(sizeof(struct tcphdr) / 2); i++) sum += ptr[i];

    // Add Payload
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
    return (unsigned short)(~sum);
}

// --- 3. Main Application ---
int main(int argc, char *argv[]) { // <-- Fixed: char *argv[]
    char *src_ip_str = NULL;
    char *dst_ip_str = NULL;
    char *b64_payload = NULL;

    // Default TCP configuration
    uint16_t sport = 12345, dport = 80, win = 5840;
    uint32_t seq = 0, ack = 0;
    int f_syn = 0, f_ack = 0, f_fin = 0, f_rst = 0, f_psh = 0, f_urg = 0;
    int send_flag = 0;

    // Fixed: Declared as an array to satisfy getopt_long
    static struct option long_options[] = {
        {"src",      required_argument, 0, 's'},
        {"dst",      required_argument, 0, 'd'},
        {"sport",    required_argument, 0, 'S'},
        {"dport",    required_argument, 0, 'D'},
        {"seq",      required_argument, 0, 'q'},
        {"ack",      required_argument, 0, 'a'},
        {"win",      required_argument, 0, 'w'},
        {"data",     required_argument, 0, 'b'},
        {"syn",      no_argument,       0, 'Y'},
        {"ack-flag", no_argument,       0, 'A'},
        {"fin",      no_argument,       0, 'F'},
        {"rst",      no_argument,       0, 'R'},
        {"psh",      no_argument,       0, 'P'},
        {"urg",      no_argument,       0, 'U'},
        {"send",     no_argument,       0, 'x'},
        {0, 0, 0, 0}
    };

    int opt;
    while ((opt = getopt_long(argc, argv, "s:d:S:D:q:a:w:b:YAFRPUx", long_options, NULL)) != -1) {
        switch (opt) {
            case 's': src_ip_str = optarg; break;
            case 'd': dst_ip_str = optarg; break;
            case 'S': sport = (uint16_t)atoi(optarg); break;
            case 'D': dport = (uint16_t)atoi(optarg); break;
            case 'q': seq = (uint32_t)atoll(optarg); break;
            case 'a': ack = (uint32_t)atoll(optarg); break;
            case 'w': win = (uint16_t)atoi(optarg); break;
            case 'b': b64_payload = optarg; break;
            case 'Y': f_syn = 1; break;
            case 'A': f_ack = 1; break;
            case 'F': f_fin = 1; break;
            case 'R': f_rst = 1; break;
            case 'P': f_psh = 1; break;
            case 'U': f_urg = 1; break;
            case 'x': send_flag = 1; break;
            default: exit(EXIT_FAILURE);
        }
    }

    if (!src_ip_str || !dst_ip_str) {
        fprintf(stderr, "[!] Both --src and --dst are mandatory for the Pseudo Header.\n");
        exit(EXIT_FAILURE);
    }

    // Fixed: Using array instead of `char packet;`
    char packet[MAX_PACKET_SIZE];
    memset(packet, 0, sizeof(packet));

    // Map memory sections
    struct iphdr *iph = (struct iphdr *)packet;
    struct tcphdr *tcph = (struct tcphdr *)(packet + sizeof(struct iphdr));
    unsigned char *payload_ptr = (unsigned char *)(packet + sizeof(struct iphdr) + sizeof(struct tcphdr));
    size_t payload_len = 0;

    if (b64_payload != NULL) {
        payload_len = base64_decode(b64_payload, payload_ptr);
    }

    // --- 4. Build IP Header ---
    // (Required so raw sockets allow spoofing the source IP)
    iph->version = 4;
    iph->ihl = 5;
    iph->tot_len = htons(sizeof(struct iphdr) + sizeof(struct tcphdr) + payload_len);
    iph->id = htons(54321);
    iph->ttl = 64;
    iph->protocol = IPPROTO_TCP;
    inet_pton(AF_INET, src_ip_str, &(iph->saddr));
    inet_pton(AF_INET, dst_ip_str, &(iph->daddr));
    iph->check = calculate_checksum((unsigned short *)iph, iph->ihl * 4);

    // --- 5. Build TCP Header ---
    tcph->source = htons(sport);
    tcph->dest = htons(dport);
    tcph->seq = htonl(seq);
    tcph->ack_seq = htonl(ack);
    tcph->doff = 5; // 5 * 4 = 20 bytes

    // Set Handshake Flags
    tcph->syn = f_syn;
    tcph->ack = f_ack;
    tcph->fin = f_fin;
    tcph->rst = f_rst;
    tcph->psh = f_psh;
    tcph->urg = f_urg;
    tcph->window = htons(win);
    tcph->check = 0; // Set to 0 before calculation

    // Prepare Pseudo Header
    struct pseudo_header psh;
    psh.src_addr = iph->saddr;
    psh.dst_addr = iph->daddr;
    psh.zero = 0;
    psh.protocol = IPPROTO_TCP;
    psh.tcp_length = htons(sizeof(struct tcphdr) + payload_len);

    // Calculate TCP Checksum
    tcph->check = calculate_tcp_checksum(&psh, tcph, payload_ptr, payload_len);

    // --- 6. Execution Branching ---
    if (!send_flag) {
        // Output strictly formatted JSON for Kotlin integration
        printf("{\n");
        printf("  \"Forged\": true,\n");
        printf("  \"tcp_header\": {\n");
        printf("    \"sport\": %u,\n", ntohs(tcph->source));
        printf("    \"dport\": %u,\n", ntohs(tcph->dest));
        printf("    \"seq\": %u,\n", ntohl(tcph->seq));
        printf("    \"ack\": %u,\n", ntohl(tcph->ack_seq));
        printf("    \"flags\": {\"SYN\": %d, \"ACK\": %d, \"FIN\": %d, \"RST\": %d, \"PSH\": %d, \"URG\": %d},\n",
                tcph->syn, tcph->ack, tcph->fin, tcph->rst, tcph->psh, tcph->urg);
        printf("    \"window\": %u,\n", ntohs(tcph->window));
        printf("    \"checksum\": %u\n", ntohs(tcph->check));
        printf("  },\n");
        printf("  \"payload_len\": %zu\n", payload_len);
        printf("}\n");
        fflush(stdout);
    } else {
        // IPPROTO_RAW explicitly tells the kernel "I already built the IP header, just send the packet"
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