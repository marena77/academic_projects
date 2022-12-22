CC = gcc
CFLAGS = -Wall
all:
	make server

server: server.c
	$(CC) $(CFLAGS) server.c -o rserver -lssl -lpthread

clean:
	rm -f server
