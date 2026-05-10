What is application will look like?

I want it to be CLI for the first time, then maybe little UI

CLI: user actions sequence to perform p2p communication:

1. public nodes
    * list public nodes
    * select some
    * find user
2. communication:
    * send text to user
    * wait for response

What should I do?
1. simple libp2p layer of public nodes, preferably 1 real public node, and remember it
2. application itself must be libp2p node
3. modify node to use utility to forge packets, so send spoofed ones
4. receive as usual libp2p app
