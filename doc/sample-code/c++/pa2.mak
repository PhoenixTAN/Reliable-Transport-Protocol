CC          = gcc
LIBDIR      = /arch/gnu/lib
LIBFLAGS    = -L${LIBDIR}
CFLAGS      = $(LIBFLAGS) -g 
GCCLIBS     = -lstdc++

OBJS = pa2.o 
EXE = pa2

all: ${EXE}

%.o: %.cc
	${CC} ${CFLAGS} -c $*.cc

${EXE}: ${OBJS}
	${CC} ${CFLAGS} -o ${EXE} ${OBJS} ${GCCLIBS}

pa2.o: pa2.cc

clean:
	rm -f ${EXE} ${OBJS} *~
     
