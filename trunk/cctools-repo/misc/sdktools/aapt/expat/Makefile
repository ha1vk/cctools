TARGET	= libexpat.a

OBJS = \
	xmlparse.o \
	xmlrole.o \
	xmltok.o

CFLAGS = \
	-O2 \
	-Wall \
	-Wmissing-prototypes -Wstrict-prototypes \
	-Wno-unused-parameter -Wno-missing-field-initializers \
	-fexceptions \
	-DHAVE_EXPAT_CONFIG_H \
	-I../include/libexpat \
	-I.

ifeq (arm,$(CPU))
CFLAGS += -march=armv6
endif

$(TARGET): $(OBJS)
	$(AR) rcs $@ $^

clean:
	rm -f $(OBJS) $(TARGET)
