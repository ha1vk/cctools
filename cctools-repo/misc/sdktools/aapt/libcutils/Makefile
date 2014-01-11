TARGET	= libcutils.a

OBJS = \
	hashmap.o \
	atomic.o \
	native_handle.o \
	socket_inaddr_any_server.o \
	socket_local_client.o \
	socket_local_server.o \
	socket_loopback_client.o \
	socket_loopback_server.o \
	socket_network_client.o \
	sockets.o \
	config_utils.o \
	cpu_info.o \
	load_file.o \
	list.o \
	open_memstream.o \
	strdup16to8.o \
	strdup8to16.o \
	record_stream.o \
	process_name.o \
	threads.o \
	sched_policy.o \
	iosched_policy.o \
	str_parms.o \
	ashmem-host.o \
	dlmalloc_stubs.o

CFLAGS = \
	-O2 \
	-Wall \
	-DOS_PATH_SEPARATOR=\'/\' \
	-DFAKE_LOG_DEVICE=1 \
	-DHAVE_PTHREADS \
	-DHAVE_OFF64_T \
	-DHAVE_ENDIAN_H \
	-DHAVE_SYS_UIO_H \
	-DHAVE_POSIX_FILEMAP \
	-DSTATIC_ANDROIDFW_FOR_TOOLS \
	-DANDROID_SMP=0 \
	-I../include

ifeq (arm,$(CPU))
CFLAGS += -march=armv6
endif

CXXFLAGS = $(CFLAGS)

$(TARGET): $(OBJS)
	$(AR) rcs $@ $^

clean:
	rm -f $(OBJS) $(TARGET)
