#include <stdio.h>
#include <stdint.h>
#include <string.h>

#include "tcp.h"

enum {
    FUNC_NONE = 0,
    FUNC_YESNO,
    FUNC_MSGBOX,
    FUNC_EDITBOX,
};

int main(int argc, char *argv[])
{
    int r;
    int func = FUNC_NONE;
    char *title = NULL;
    char *message = NULL;
    char *text = NULL;

    while (--argc > 0) {
	if (!strcmp(argv[1], "--yesno"))
	    func = FUNC_YESNO;
	else if (!strcmp(argv[1], "--msgbox"))
	    func = FUNC_MSGBOX;
	else if (!strcmp(argv[1], "--editbox"))
	    func = FUNC_EDITBOX;
	else if (!strcmp(argv[1], "--title")) {
	    title = argv[2];
	    argc--;
	    argv++;
	} else if (!strcmp(argv[1], "--message")) {
	    message = argv[2];
	    argc--;
	    argv++;
	} else if (!strcmp(argv[1], "--text")) {
	    text = argv[2];
	    argc--;
	    argv++;
	} else {
	    fprintf(stderr, "Unknown arg %s", argv[1]);
	    return -1;
	}
	argv++;
    }


    tcp_channel *server = tcp_open(TCP_CLIENT, "192.168.1.205", 13527);
    if (!server) {
	fprintf(stderr, "tcp_open()\n");
	return -1;
    }

    if (func == FUNC_YESNO) {
	char buf[1024];
	snprintf(buf, sizeof(buf) - 1, "yesno\n%s\n%s\n", title, message);
	if ((r = tcp_write(server, buf, strlen(buf))) <= 0) {
	    fprintf(stderr, "tcp_write()\n");
	    return -1;
	}

	if ((r = tcp_read(server, buf, sizeof(buf))) > 0) {
//	    fprintf(stderr, "buf[%d]=%s\n", r, buf);
	    printf("%s", buf);
	    printf("1%c", buf[0]);
	    printf("2%c", buf[1]);
	    printf("3%c", buf[2]);
	    printf("4%c", buf[3]);
	    printf("5%c", buf[4]);
	    printf("6%c", buf[5]);
	    printf("7%c", buf[6]);
	    printf("8%c", buf[7]);
	}
	fprintf(stderr, "ret=%d\n", r);
    }

/*
    strcpy(buf, "Hello server!");

    if ((r = tcp_write(server, (uint8_t *)buf, strlen(buf) + 1)) <= 0) {
	fprintf(stderr, "tcp_write()\n");
    }

    if ((r = tcp_read(server, (uint8_t *)buf, BUF_SIZE)) > 0) {
	fprintf(stderr, "buf[%d]=%s\n", r, buf);
    }
*/

    tcp_close(server);

    return 0;
}

