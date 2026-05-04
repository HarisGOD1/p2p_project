# p2p_project

guide:
1. nat_models is for NATing router simulations
   1. here will be services, which will do packet forwarding by NAT:
   2. Full Cone, Address-Restricted, Address-Port-Restricted , Symmetric; NAT types
2. client_models is for client, suffers from NAT
   1. 
   2. depending on ISP's NAT type
   3. will break it using matching traversal utils 
3. traversal_utils is for technics, to go through NAT
   1. different technics for each ISP's NAT type
      1. WebRTC-like
      2. IpV4 over IpV6
      3. random adr:port pairing for Sym NAT
   2. will be used by clients

### communication sequence

1. meet in lobby
2. organize a strategy
3. accept or deny:
   1. reorganize a strategy (return to 2.)
   2. init communication
4. end of communication

