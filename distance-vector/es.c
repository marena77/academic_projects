#ifndef _ES_C_
#define _ES_C_

/* $Id: es.c,v 1.1 2000/03/01 14:09:09 bobby Exp bobby $
 * ----
 * $Revision: 1.1 $
 */

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <sys/time.h>
#include <string.h>
#include <signal.h>
#include <sys/types.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>

#include "queue.h"
#include "common.h"
#include "es.h"
#include "ls.h"
#include "rt.h"
#include "n2h.h"

static struct el* g_el;

static int upd_time;

int init_new_el()
{
	InitDQ(g_el, struct el);
	assert (g_el);

	g_el->es_head = 0x0;
	return (g_el != 0x0);
}

void add_new_es()
{
	struct es* n_es;
	struct el* n_el = (struct el*)
		getmem (sizeof(struct el));

	struct el* tail = g_el->prev;
	InsertDQ(tail, n_el);

	// added new es to tail
	// lets start a new queue here

	{
		struct es* nhead = tail->es_head;
		InitDQ(nhead, struct es);

		tail = g_el->prev;
		tail->es_head = nhead;

		n_es = nhead;

		n_es->ev = _es_null;
		n_es->peer0 = n_es->peer1 =
			n_es->port0 = n_es->port1 =
			n_es->cost = -1;
		n_es->name = 0x0;
	}
}

void add_to_last_es(e_type ev,
	node peer0, int port0,
	node peer1, int port1,
	int cost, char *name)
{
	struct el* tail = g_el->prev;
	bool local_event = false;

	assert (tail->es_head);

	// check for re-defined link (for establish)
	// check for local event (for tear-down, update)
	switch (ev) {
		case _es_link:
			// a local event?
			if ((peer0 == get_myid()) || peer1 == get_myid())
				local_event = true;
			break;
		case _ud_link:
			// a local event?
			if (geteventbylink(name))
				local_event = true;
			break;
		case _td_link:
			// a local event?
			if (geteventbylink(name))
				local_event = true;
			break;
		default:
			printf("[es]\t\tUnknown event!\n");
			break;
	}

	if (!local_event) {
		printf("[es]\t Not a local event, skip\n");
		return;
	}

	printf("[es]\t Adding into local event\n");

	{
		struct es* es_tail = (tail->es_head)->prev;

		struct es* n_es = (struct es*)
			getmem (sizeof(struct es));

		n_es->ev = ev;
		n_es->peer0 = peer0;
		n_es->port0 = port0;
		n_es->peer1 = peer1;
		n_es->port1 = port1;
		n_es->cost = cost;
		n_es->name = (char *)
			getmem(strlen(name)+1);
		strcpy (n_es->name, name);

		InsertDQ (es_tail, n_es);
	}
}



/*
 * A simple walk of event sets: dispatch and print a event SET every 2 sec
 */
void walk_el(int update_time, int time_between, int verb)
{
	struct el *el;
	struct es *es_hd;
	struct es *es;

  /* you will be using these variables, but for now ignoring them */
  // UNUSED(update_time);
  // UNUSED(time_between);
  UNUSED(verb);

	assert (g_el->next);

	print_el();

	/* initialize link set, routing table, and routing table */
	create_ls();
	create_rt();
	init_rt_from_n2h();






	// add myself to routing table
	add_rte(get_myid(), 0, get_myid());



	for (el = g_el->next ; el != g_el ; el = el->next) {
		assert(el);
		es_hd = el->es_head;
		assert (es_hd);


		printf("[es] >>>>>>>>>> Dispatch next event set <<<<<<<<<<<<<\n");
		for (es=es_hd->next ; es!=es_hd ; es=es->next) {
			printf("[es] Dispatching next event ... \n");
			if (es->ev == _es_link) {
				if (es->peer0 == get_myid()) {
					if (find_rte(es->peer1)) {
						// already an rt entry but just got initialized
						update_rte(es->peer1, es->cost, es->peer1);
					} else {
						// no rt entry for this link
						add_rte(es->peer1, es->cost, es->peer0);
					}
				} else {
					if (find_rte(es->peer0)) {
						update_rte(es->peer0, es->cost, es->peer0);
					} else {
						add_rte(es->peer0, es->cost, es->peer1);
					}
				}
			} else if (es->ev == _td_link) {

				struct link *l = find_link(es->name);

				if (l->peer0 == get_myid()) {
					del_rte(l->peer1);
				} else {
					del_rte(l->peer0);
				}
			} else { // es->ev is _ud_link but not checking

				if (es->peer0 == get_myid()) {
					update_rte(es->peer1, es->cost, es->peer0);
				} else {
					update_rte(es->peer0, es->cost, es->peer1);
				}
			}
			dispatch_event(es);
		}

		/*
		 * ---------------------------------------------------------
		 * Run DISTANCE VECTOR ALGORITHM here
		 * ---------------------------------------------------------
		 */

		if (fork() == 0) {
			fd_set fds;
			int *socks = conn_links();
			int maxfd = 0;
			for (int s = 0; s < num_links(); s++) {
				FD_SET(socks[s], &fds);
				if (socks[s] > maxfd) {
					maxfd = socks[s];
				}
			}
			select(maxfd, &fds, NULL, NULL, NULL);
			char buffer[200];
			bzero(buffer, 200);
			for (int s = 0; s < num_links(); s++) {
				if (FD_ISSET(socks[s], &fds)) {
					recvfrom(socks[s], buffer, sizeof(buffer), 0, NULL, NULL);
				}
				printf("New buffer:\n");
				for (int i = 0; i < sizeof(buffer); i++) {
					printf("%x\n",buffer[i]);
				}
				printf("\n");
			}
			exit(EXIT_SUCCESS);
		}

		// is the following code generalizable to cases after first event set?
		int num_updates = num_rtes();
 		unsigned char packet[4+(4*num_updates)];
		bzero(packet, 4+(4*num_updates));
 		//setting type and version
 		packet[0] = (unsigned char)7;
 		packet[1] = (unsigned char)1;


 		packet[2] = (num_updates >> 8) & 0xFF;
 		packet[3] = num_updates & 0xFF;

 		struct rte *i = find_rte(get_myid());
 		int update_count = 1;

 		for (i=i; i->c != -1 && i->nh != -1 && i->d != -1; i = i->next) {

			// print_rte(i);
 			// dest field
 			packet[(update_count*4)] = (char)i->d;

 			packet[(update_count*4)+ 1] = (i->c >> 16) & 0xFF;
 			packet[(update_count*4)+ 2] = (i->c >> 8) & 0xFF;
 			packet[(update_count*4)+ 3] = (i->c) & 0xFF;

 			update_count++;
 		}

		// for (int i = 0; i < sizeof(packet); i++) {
		// 	printf("%x\n",packet[i]);
		// }

		// get directly connected nodes
		// use hp = gethostbyname(gethostbynode(nid)), then hp->h_addr for adress
		// make socket, then send them all the packet
		int *links = send_links();
		for (int p = 0; p < num_links(); p++) {
			int s = socket(AF_UNSPEC, SOCK_DGRAM, IPPROTO_UDP);
			struct hostent *hp;
			hp = gethostbyname(gethostbynode(links[p]));
			sendto(s, packet, sizeof(packet), 0, hp->h_addr, hp->h_length);
		}



		printf("[es] >>>>>>> Start dumping data stuctures <<<<<<<<<<<\n");
		print_n2h();
		print_ls();
		print_rt();
	}
}


/*
 * -------------------------------------
 * Dispatch one event
 * -------------------------------------
 */
void dispatch_event(struct es* es)
{
	assert(es);

	switch (es->ev) {
		case _es_link:
			add_link(es->peer0, es->port0, es->peer1, es->port1,
				es->cost, es->name);
			break;
		case _ud_link:
			ud_link(es->name, es->cost);
			break;
		case _td_link:
			del_link(es->name);
			break;
		default:
			printf("[es]\t\tUnknown event!\n");
			break;
	}

}

/*
 * print out the whole event LIST
 */
void print_el()
{
	struct el *el;
	struct es *es_hd;
	struct es *es;

	assert (g_el->next);

	printf("\n\n");
	printf("[es] >>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<\n");
	printf("[es] >>>>>>>>>> Dumping all event sets  <<<<<<<<<<<<<\n");
	printf("[es] >>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<\n");

	for (el = g_el->next ; el != g_el ; el = el->next) {

		assert(el);
		es_hd = el->es_head;
		assert (es_hd);

		printf("\n[es] ***** Dumping next event set *****\n");

		for (es=es_hd->next ; es!=es_hd ; es=es->next)
			print_event(es);
	}
}

/*
 * print out one event: establish, update, or, teardown
 */
void print_event(struct es* es)
{
	assert(es);

	switch (es->ev) {
		case _es_null:
			printf("[es]\t----- NULL event -----\n");
			break;
		case _es_link:
			printf("[es]\t----- Establish event -----\n");
			break;
		case _ud_link:
			printf("[es]\t----- Update event -----\n");
			break;
		case _td_link:
			printf("[es]\t----- Teardown event -----\n");
			break;
		default:
			printf("[es]\t----- Unknown event-----\n");
			break;
	}
	printf("[es]\t link-name(%s)\n",es->name);
	printf("[es]\t node(%d)port(%d) <--> node(%d)port(%d)\n",
		es->peer0,es->port0,es->peer1,es->port1);
	printf("[es]\t cost(%d)\n", es->cost);
}

struct es *geteventbylink(char *lname)
{
	struct el *el;
	struct es *es_hd;
	struct es *es;

	assert (g_el->next);
	assert (lname);

	for (el = g_el->next ; el != g_el ; el = el->next) {

		assert(el);
		es_hd = el->es_head;
		assert (es_hd);

		for (es=es_hd->next ; es!=es_hd ; es=es->next)
			if (!strcmp(lname, es->name))
				return es;
	}
	return 0x0;
}

#endif
