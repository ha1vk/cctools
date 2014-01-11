TARGET	= libutils.a

OBJS = \
	BasicHashtable.o \
	BlobCache.o \
	CallStack.o \
	FileMap.o \
	JenkinsHash.o \
	LinearAllocator.o \
	LinearTransform.o \
	Log.o \
	Looper.o \
	Printer.o \
	ProcessCallStack.o \
	PropertyMap.o \
	RefBase.o \
	SharedBuffer.o \
	Static.o \
	StopWatch.o \
	String8.o \
	String16.o \
	SystemClock.o \
	Threads.o \
	Timers.o \
	Tokenizer.o \
	Unicode.o \
	VectorImpl.o \
	misc.o

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
	-I../include

ifeq (mips, $(CPU))
CFLAGS += -DALIGN_DOUBLE
endif

ifeq (arm,$(CPU))
CFLAGS += -march=armv6
endif

CXXFLAGS = $(CFLAGS)

$(TARGET): $(OBJS)
	$(AR) rcs $@ $^

clean:
	rm -f $(OBJS) $(TARGET)
