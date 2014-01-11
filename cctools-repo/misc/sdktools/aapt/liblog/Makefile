TARGET	= liblog.a

OBJS = \
	logd_write.o \
	fake_log_device.o \
	uio.o

CFLAGS = \
	-O2 \
	-Wall \
	-DFAKE_LOG_DEVICE=1 \
	-DHAVE_PTHREADS \
	-DHAVE_OFF64_T \
	-DHAVE_ENDIAN_H \
	-DHAVE_SYS_UIO_H \
	-I../include

ifeq (arm,$(CPU))
CFLAGS += -march=armv6
endif

CXXFLAGS = $(CFLAGS)

$(TARGET): $(OBJS)
	$(AR) rcs $@ $^

clean:
	rm -f $(OBJS) $(TARGET)
