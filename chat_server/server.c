#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

#include <argp.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <openssl/evp.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <pthread.h>
#include <signal.h>

struct server_arguments {
	int port;
};


error_t server_parser(int key, char *arg, struct argp_state *state) {
	struct server_arguments *args = state->input;
	error_t ret = 0;
	switch(key) {
	case 'p':
		/* Validate that port is correct and a number, etc!! */
		args->port = atoi(arg);
		if (0 /* port is invalid */) {
			argp_error(state, "Invalid option for a port, must be a number");
		}
		break;
	default:
		ret = ARGP_ERR_UNKNOWN;
		break;
	}
	return ret;
}

typedef struct {
	char name[256];
	char pass[256];
} room_t;

typedef struct {
	int connfd;
	char name[256];
	room_t *room;
} client_t;

client_t *clients[20];
int taken_defaults[20] = {0};

pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

void *handle_client(void *arg) {
	int user_assigned = 0;
	client_t *cli = (client_t *)arg;

	unsigned char b[65803];
	bzero(b, 48);
	//read in "milk and honig msg"
	int bytes_read = read(cli->connfd, b, 48);
	//verify magic numbers


	while (1) {
		//eventually I need to figure out how large b must be
		//in order to account for maximum incoming message length
		bzero(b, 65803);
		//read in staying alive
		bytes_read = read(cli->connfd, b, 65803);
		printf("Read %d more bytes\n",bytes_read);
		if (bytes_read > 65802) {
			unsigned char w[30];
			bzero(w, 30);
			w[3] = 0x17;
			w[4] = 0x04;
			w[5] = 0x17;
			w[6] = 0x9a;
			w[7] = 0x01;
			memcpy(&w[8], "Length limit exceeded.", 22);
			write(cli->connfd, w, sizeof(w));
			close(cli->connfd);

			// check if name was a default and update taken_defaults
			int n_len = strlen(cli->name);
			char prefix[5];
			prefix[4] = '\0';
			memcpy(prefix, cli->name, 4);
			if (n_len == 5 || n_len == 6) {
				if (!strcmp(prefix, "rand")) {
					long int num = strtol(&cli->name[4], NULL, 10);
					if (0 <= num && num <= 19) {
						pthread_mutex_lock(&mutex);
						taken_defaults[num] = 0;
						pthread_mutex_unlock(&mutex);
					}
				}
			}

			pthread_mutex_lock(&mutex);
			for (int i = 0; i < 20; i++) {
				if (clients[i]) {
					if (!strcmp(cli->name, clients[i]->name)) {
						clients[i] = NULL;
						break;
					}
				}
			}
			pthread_mutex_unlock(&mutex);

			free(cli);
			pthread_detach(pthread_self());
			break;
		}
		if (!bytes_read) {
			close(cli->connfd);

			// check if name was a default and update taken_defaults
			int n_len = strlen(cli->name);
			char prefix[5];
			prefix[4] = '\0';
			memcpy(prefix, cli->name, 4);

			if (n_len == 5 || n_len == 6) {
				if (!strcmp(prefix, "rand")) {
					long int num = strtol(&cli->name[4], NULL, 10);
					if (0 <= num && num <= 19) {
						pthread_mutex_lock(&mutex);
						taken_defaults[num] = 0;
						pthread_mutex_unlock(&mutex);
					}
				}
			}

			pthread_mutex_lock(&mutex);
			for (int i = 0; i < 20; i++) {
				if (clients[i]) {
					if (!strcmp(cli->name, clients[i]->name)) {
						clients[i] = NULL;
						break;
					}
				}
			}
			pthread_mutex_unlock(&mutex);

			free(cli);
			pthread_detach(pthread_self());
			break;
		}

		// sort client list for \list users purposes
		pthread_mutex_lock(&mutex);
		client_t *temp;
		for (int i = 0; i < 20; i++) {
	        for (int j = 0; j < 20; j++) {
	            if (clients[i] && clients[j]) {
	                if(strcmp(clients[i]->name, clients[j]->name)<0) {
	                    temp = clients[i];
	                    clients[i] = clients[j];
	                    clients[j] = temp;
	                }
	            }
	        }
		}
		pthread_mutex_unlock(&mutex);

		//keep-alive magic numbers
		if (b[3] == 0x20 && b[4] == 0x04 && b[5] == 0x17 && b[6] == 0x13 && b[7] == 0x1f) {
			// printf("got keep alive\n");
		//nick name
		} else if (b[4] == 0x04 && b[5] == 0x17 && b[6] == 0x0f) {
			//check if we need to update taken_defaults

			int n_len = strlen(cli->name);
			char prefix[4];
			memcpy(prefix, cli->name, 4);
			if (n_len == 5 || n_len == 6) {
				if (!strcmp(prefix, "rand")) {
					long int num = strtol(&cli->name[4], NULL, 10);
					if (0 <= num && num <= 19) {
						pthread_mutex_lock(&mutex);
						taken_defaults[num] = 0;
						pthread_mutex_unlock(&mutex);
					}
				}
			}
			//update our username data structure
			bzero(cli->name, 256);
			strcpy(cli->name, &b[8]);

			//write back confirmation
			unsigned char w[8];
			bzero(w, 8);
			w[3] = 0x01;
			w[4] = 0x04;
			w[5] = 0x17;
			w[6] = 0x9a;
			write(cli->connfd, w, sizeof(w));
		//list users
		} else if (b[3] == 0x0 && b[4] == 0x04 && b[5] == 0x17 && b[6] == 0x0c) {
			//get number of users
			if (cli->room == NULL) {
				int count = 0;
				int total_len = 0;
				for (int i = 0; i < 20; i++) {
					if (clients[i]) {
						count++;
						total_len += (strlen(clients[i]->name) + 1);
					}
				}
				count = total_len+1;

				unsigned char w[8+total_len];
				bzero(w, 8+total_len);
				// amount in list
				w[0] = (count >> 24) & 0xFF;
				w[1] = (count >> 16) & 0xFF;
				w[2] = (count >> 8) & 0xFF;
				w[3] = count & 0xFF;
				// magic numbers
				w[4] = 0x04;
				w[5] = 0x17;
				w[6] = 0x9a;

				int marker_count = 0;
				for (int i = 0; i < 20; i++) {
					if (clients[i]) {
						int n_len = strlen(clients[i]->name);
						w[8+marker_count] = (char)n_len;
						memcpy(&w[9+marker_count],clients[i]->name,n_len);
						marker_count += (n_len + 1);
					}
				}

				write(cli->connfd, w, sizeof(w));
			} else {
				int total_len = 0;
				int count = 0;
				for (int i = 0; i < 20; i++) {
					if (clients[i] && clients[i]->room) {
						if (!strcmp(cli->room->name, clients[i]->room->name)) {
							count++;
							total_len += (strlen(clients[i]->name) + 1);
						}
					}
				}
				count = total_len+1;

				unsigned char w[8+total_len];
				bzero(w, 8+total_len);
				// amount in list
				w[0] = (count >> 24) & 0xFF;
				w[1] = (count >> 16) & 0xFF;
				w[2] = (count >> 8) & 0xFF;
				w[3] = count & 0xFF;
				// magic numbers
				w[4] = 0x04;
				w[5] = 0x17;
				w[6] = 0x9a;

				int marker_count = 0;
				for (int i = 0; i < 20; i++) {
					if (clients[i] && clients[i]->room) {
						if (!strcmp(cli->room->name, clients[i]->room->name)) {
							int n_len = strlen(clients[i]->name);
							w[8+marker_count] = (char)n_len;
							memcpy(&w[9+marker_count],clients[i]->name,n_len);
							marker_count += (n_len + 1);
						}
					}
				}
				write(cli->connfd, w, sizeof(w));
			}
		// list rooms
		} else if (b[4] == 0x04 && b[5] == 0x17 && b[6] == 0x09) {
			int t_rn_len = 0;
			int count = 0;
			room_t *rooms[20];
			for (int i = 0; i < 20; i++) {
				if (clients[i] && clients[i]->room) {
					t_rn_len += strlen(clients[i]->room->name);
					count++;
					rooms[i] = clients[i]->room;
				}
			}
			//sort rooms
			room_t *temp;
			for (int i = 0; i < 20; i++) {
				for (int j = 0; j < 20; j++) {
					if (rooms[i] && rooms[j]) {
						if(strcmp(rooms[i]->name, rooms[j]->name)<0) {
		                    temp = rooms[i];
		                    rooms[i] = rooms[j];
		                    rooms[j] = temp;
		                }
					}
				}
			}
			unsigned char w[8+count+t_rn_len];
			bzero(w, 8+count+t_rn_len);
			w[0] = ((count+t_rn_len+1) >> 24) & 0xFF;
			w[1] = ((count+t_rn_len+1) >> 16) & 0xFF;
			w[2] = ((count+t_rn_len+1) >> 8) & 0xFF;
			w[3] = (count+t_rn_len+1) & 0xFF;
			w[4] = 0x04;
			w[5] = 0x17;
			w[6] = 0x9a;
			int marker_count = 0;
			for (int i = 0; i < 20; i++) {
				if (rooms[i]) {
					int r_len = strlen(rooms[i]->name);
					w[8+marker_count] = (char)r_len;
					memcpy(&w[9+marker_count],rooms[i]->name,r_len);
					marker_count += (r_len+1);
				}
			}
			write(cli->connfd, w, sizeof(w));
		// join room
		} else if (b[4] == 0x04 && b[5] == 0x17 && b[6] == 0x03) {
			// 4th byte is number bytes after last magic number
			// last byte is 0 if no password
			char room_name[256];
			int room_length = (int)(b[7]);
			memcpy(room_name, &b[8], room_length);

			room_t *room = NULL;
			for (int i = 0; i < 20; i++) {
				if (clients[i] && clients[i]->room) {
					if (!strcmp(room_name, clients[i]->room->name)) {
						room = clients[i]->room;
					}
				}
			}

			char pass[256];
			int pass_length = (int)(b[8+room_length]);
			memcpy(pass, &b[9+room_length], pass_length);

			if (room != NULL) {

				// it exists, so validate password
				if (!strcmp(pass, room->pass)) {
					// send confirmation and update client room
					cli->room = room;

					unsigned char w[8];
					bzero(w, 8);
					w[3] = 0x01;
					w[4] = 0x04;
					w[5] = 0x17;
					w[6] = 0x9a;

					write(cli->connfd, w, sizeof(w));

				} else {
					// send you shall not pass
					unsigned char w[45];
					bzero(w, 45);
					w[3] = 0x26;
					w[4] = 0x04;
					w[5] = 0x17;
					w[6] = 0x9a;
					w[7] = 0x01;
					memcpy(&w[8], "Invalid password. You shall not pass.", 37);

					write(cli->connfd, w, sizeof(w));
				}


			} else {

				// room doesn't exist so create it and update client
				room_t *new_room = (room_t *)malloc(sizeof(room_t));
				memcpy(new_room->name, room_name, sizeof(room_name));
				memcpy(new_room->pass, pass, sizeof(pass));

				cli->room = new_room;

				unsigned char w[8];
				bzero(w, 8);
				w[3] = 0x01;
				w[4] = 0x04;
				w[5] = 0x17;
				w[6] = 0x9a;

				write(cli->connfd, w, sizeof(w));

			}
		//leave room
		} else if (b[4] == 0x04 && b[5] == 0x17 && b[6] == 0x06) {

			unsigned char w[8];
			bzero(w, 8);
			w[3] = 0x01;
			w[4] = 0x04;
			w[5] = 0x17;
			w[6] = 0x9a;
			write(cli->connfd, w, sizeof(w));
			if (cli->room == NULL) {
				close(cli->connfd);
				free(cli);
				pthread_detach(pthread_self());
			} else {
				int flag = 0;
				// checking if anyone else is in the room
				for (int i = 0; i < 20; i++) {
					if (clients[i] && clients[i]->room) {
						if (!strcmp(clients[i]->room->name, cli->room->name)) {
							if (clients[i]->connfd != cli->connfd) {
								flag = 1;
							}
						}
					}
				}
				if (flag) {
					cli->room = NULL;
				} else {
					// printf("Freeing room\n");
					free(cli->room);
					cli->room = NULL;
				}
			}
		// message broadcast
		} else if (b[4] == 0x04 && b[5] == 0x17 && b[6] == 0x15) {
			if (cli->room == NULL) {
				unsigned char w[49];
				bzero(w, 49);
				w[3] = 0x2a;
				w[4] = 0x04;
				w[5] = 0x17;
				w[6] = 0x9a;
				w[7] = 0x01;
				memcpy(&w[8], "You shout into the void and hear nothing.", 41);
				write(cli->connfd, w, sizeof(w));
			} else {
				//write back confirmation to self
				unsigned char w[8];
				bzero(w, 8);
				w[3] = 0x01;
				w[4] = 0x04;
				w[5] = 0x17;
				w[6] = 0x9a;
				write(cli->connfd, w, sizeof(w));

				int room_length = (int)b[7];
				int msg_length = ((int)b[8+room_length] << 8) + ((int)b[9+room_length]);

				int in_len = b[3] + (b[2] << 8) + (b[1] << 16) + (b[0] << 24);
				if ((bytes_read-7) != in_len) {
					read(cli->connfd, &b[bytes_read], in_len-bytes_read);
				}

				for (int i = 0; i < 20; i++) {
					if (clients[i] && clients[i]->room) {
						if (!strcmp(clients[i]->room->name, cli->room->name)) {
							if (strcmp(clients[i]->name, cli->name)) {
								int n_len = strlen(cli->name);
								int resp_len = 11+room_length+n_len+msg_length;
								unsigned char r[resp_len];
								bzero(r, resp_len);
								int t_length = 4+room_length+msg_length+n_len;
								r[0] = (char)((t_length >> 24) & 0xFF);
								r[1] = (char)((t_length >> 16) & 0xFF);
								r[2] = (char)((t_length >> 8) & 0xFF);
								r[3] = (char)(t_length & 0xFF);
								r[4] = 0x04;
								r[5] = 0x17;
								r[6] = 0x15;
								r[7] = (char)room_length;

								memcpy(&r[8], cli->room->name, room_length);
								r[8+room_length] = (char)n_len;
								memcpy(&r[9+room_length], cli->name, n_len);
								r[9+room_length+n_len] = b[8+room_length];
								r[10+room_length+n_len] = b[9+room_length];
								memcpy(&r[11+room_length+n_len], &b[10+room_length], msg_length);

								pthread_mutex_lock(&mutex);
								write(clients[i]->connfd, r, resp_len);
								pthread_mutex_unlock(&mutex);

							}
						}
					}
				}

			}
		// private msg
	} else if (b[4] == 0x04 && b[5] == 0x17 && b[6] == 0x12) {
			char r_name[256];
			bzero(r_name, 256);
			int r_name_len = (int)b[7];
			memcpy(r_name, &b[8], r_name_len);

			int in_len = b[3] + (b[2] << 8) + (b[1] << 16) + (b[0] << 24);
			if ((bytes_read-7) != in_len) {
				read(cli->connfd, &b[bytes_read], in_len-bytes_read);
			}

			//check if user exists
			int flag = 0;
			for (int i = 0; i < 20; i++) {
				if (clients[i]) {
					if (!strcmp(clients[i]->name, r_name)) {
						flag = 1;
					}
				}
			}
			// user exists
			if (flag) {
				int name_len = strlen(cli->name);
				int msg_len = ((int)b[8+name_len] << 8) + ((int)b[9+name_len]);
				unsigned char w[10+name_len+msg_len];
				bzero(w, 10+name_len+msg_len);
				int t_length = 3+name_len+msg_len;
				w[0] = (char)((t_length >> 24) & 0xFF);
				w[1] = (char)((t_length >> 16) & 0xFF);
				w[2] = (char)((t_length >> 8) & 0xFF);
				w[3] = (char)(t_length & 0xFF);
				w[4] = 0x04;
				w[5] = 0x17;
				w[6] = 0x12;
				w[7] = (char)name_len;
				memcpy(&w[8], cli->name, name_len);
				w[8+name_len] = (msg_len >> 8) & 0xFF;
				w[9+name_len] = (msg_len) & 0xFF;
				memcpy(&w[10+name_len], &b[10+name_len], msg_len);
				for (int i = 0; i < sizeof(w); i++) {
					printf("%x ",w[i]);
				}
				printf("\n");
				//write message to recipient
				for (int i = 0; i < 20; i++) {
					if (clients[i]) {
						if (!strcmp(clients[i]->name, r_name)) {
							pthread_mutex_lock(&mutex);
							write(clients[i]->connfd, w, sizeof(w));
							pthread_mutex_unlock(&mutex);
						}
					}
				}
				// write confirmation to sender
				unsigned char r[8];
				bzero(r, 8);
				r[3] = 0x01;
				r[4] = 0x04;
				r[5] = 0x17;
				r[6] = 0x9a;
				write(cli->connfd, r, sizeof(r));
				for (int i = 0; i < sizeof(r); i++) {
					printf("%x ",r[i]);
				}
				printf("\n");

			} else {
				unsigned char w[24];
				bzero(w, 24);
				w[3] = 0x11;
				w[4] = 0x04;
				w[5] = 0x17;
				w[6] = 0x9a;
				w[7] = 0x01;
				memcpy(&w[8], "Nick not present", 16);
				write(cli->connfd, w, sizeof(w));
			}
		}

		if (!user_assigned) {
			user_assigned = 1;
			unsigned char user[13];
			bzero(user, 13);
			//magic numbers
			user[3] = (char)0x06;
			user[4] = (char)0x04;
			user[5] = (char)0x17;
			user[6] = (char)0x9a;


			memcpy(&user[8], cli->name, 5);

			write(cli->connfd, user, sizeof(user));

		}

		sleep(1);


	}
	return NULL;
}

// main function parses command line input, then
// creates, binds, listens, and accepts on socket.
// Then it initializes client structure, and passes
// it off to a pthread.

int main(int argc, char *argv[]) {
    struct server_arguments args;

	/* bzero ensures that "default" parameters are all zeroed out */
	bzero(&args, sizeof(args));

	struct argp_option options[] = {
		{ "port", 'p', "port", 0, "The port to be used for the server" ,0},
		{0}
	};
	struct argp argp_settings = { options, server_parser, 0, 0, 0, 0, 0 };
	if (argp_parse(&argp_settings, argc, argv, 0, NULL, &args) != 0) {
		printf("Got an error condition when parsing\n");
	}
	/* What happens if you don't pass in parameters? Check args values
	 * for sanity and required parameters being filled in */

	/* If they don't pass in all required settings, you should detect
	 * this and return a non-zero value from main */
	printf("Got port %d\n", args.port);

	int sockfd, connfd;
	socklen_t len;
	struct sockaddr_in servaddr, cli_s;

	/* creating socket */
	sockfd = socket(AF_INET, SOCK_STREAM, 0);
	bzero(&servaddr, sizeof(servaddr));

	servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = htonl(INADDR_ANY);
    servaddr.sin_port = htons(args.port);

	int yes = 1;

	bind(sockfd, (struct sockaddr*)&servaddr, sizeof(servaddr));
	listen(sockfd, 50);

	len = sizeof(cli_s);

	pthread_t tid;

	while (1) {
		connfd = accept(sockfd, (struct sockaddr*)&cli_s, &len);
		setsockopt(connfd, SOL_SOCKET, SO_KEEPALIVE, &yes, sizeof(int));

		client_t *cli = (client_t *)malloc(sizeof(client_t));

		pthread_mutex_lock(&mutex);
		for (int i = 0; i < 20; i++) {
			if (!taken_defaults[i]) {
				cli->name[0] = 'r';
				cli->name[1] = 'a';
				cli->name[2] = 'n';
				cli->name[3] = 'd';

				sprintf(&(cli->name[4]), "%d", i);

				taken_defaults[i] = 1;
				break;
			}
		}
		cli->connfd = connfd;
		cli->room = NULL;
		for (int i = 0; i < 20; i++) {
			if (!clients[i]) {
				clients[i] = cli;
				break;
			}
		}


		pthread_mutex_unlock(&mutex);

		pthread_create(&tid, NULL, &handle_client, (void*)cli);

	}



    return 0;
}
