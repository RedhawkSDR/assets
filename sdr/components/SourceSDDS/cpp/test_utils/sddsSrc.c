/*
 * This example was taken from https://www.cs.cmu.edu/afs/cs/academic/class/15213-f99/www/class26/udpclient.c 
 * and modified to fit the need of the SDDS UDP speed test
 */

/* 
 * udpclient.c - A simple UDP client
 * usage: udpclient <host> <port>
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h> 
#include <math.h> 

/* 
 * error - wrapper for perror
 */
void error(char *msg) {
    perror(msg);
    exit(0);
}

struct SDDS_Packet {
  unsigned dmode:3;
  unsigned ss:1;
  unsigned of:1;
  unsigned pp:1;
  unsigned sos:1;
  unsigned sf:1;

  unsigned bps:5;
  unsigned vw:1;
  unsigned snp:1;
  unsigned cx:1;  // new complex bit

  // Frame sequence
  uint16_t seq;

  // Time Tag
  uint16_t msptr; // contains [msv, ttv, sscv] bits at the top bits

  uint16_t msdel;

  uint64_t  ttag;
  uint32_t  ttage;

  // Clock info
  int32_t  dFdT;
  uint64_t  freq;
  // Odds & Ends

  uint16_t ssd[2];
  uint8_t aad[20];
  //======================== SDDS data
  uint8_t d[1024];

};

int main(int argc, char **argv) {
    int sockfd, portno, n;
    size_t target_packets;
    size_t tot_sent = 0, start_time;
    int serverlen;
    struct sockaddr_in serveraddr;
    struct hostent *server;
    char *hostname;
    float d_rate;
    size_t pkts_per_sleep = 1;
    size_t max_pkts_per_sleep = 10000;
    size_t p_sent = 0;
    struct SDDS_Packet packet = {0};
    packet.sf = 1;
    packet.ss = 1; // True for the first 2^16 packets
    packet.bps = 8; // SourceNic 
    packet.dmode = 1; // XXX: Why is this one?
    uint64_t testzero;
    uint16_t p_seq = 0;  // Internal sequence counter 
    uint64_t p_ttag = 0;

    /* check command line arguments */
    if (argc != 5) {
       fprintf(stderr,"usage: %s <hostname> <port> <target rate (Bps)> <packets sent>\n", argv[0]);
       exit(0);
    }

    hostname = argv[1];
    portno = atoi(argv[2]);
    d_rate = atof(argv[3]);
    target_packets = atoi(argv[4]);

    start_time = (unsigned)time(NULL);

    /* socket: create the socket */
    sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sockfd < 0) 
        error("ERROR opening socket");

    /* gethostbyname: get the server's DNS entry */
    server = gethostbyname(hostname);
    if (server == NULL) {
        fprintf(stderr,"ERROR, no such host as %s\n", hostname);
        exit(0);
    }

    /* build the server's Internet address */
    bzero((char *) &serveraddr, sizeof(serveraddr));
    serveraddr.sin_family = AF_INET;
    bcopy((char *)server->h_addr, 
    (char *)&serveraddr.sin_addr.s_addr, server->h_length);
    serveraddr.sin_port = htons(portno);

    /* send the message to the server */
    serverlen = sizeof(serveraddr);

    while (p_sent != target_packets) {
        if (p_sent == 20) {
            packet.freq = htonl(0);
        } else {
            packet.freq = htonl(60000000);
        }
        n = sendto(sockfd, &packet, sizeof(packet), 0, (const struct sockaddr*)(&serveraddr), serverlen);
        p_sent++;
    }
    printf("%ld\n",p_sent);
    close(sockfd);
    return 0;
}
