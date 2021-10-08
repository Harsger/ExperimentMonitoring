.PHONY : all clean

COMPILER=g++ -std=c++11 -Ofast 

SRCS := $(wildcard *.cpp)
BINS := $(SRCS:%.cpp=%)

all: ${BINS}

%: %.cpp useful.h
	${COMPILER} $< -o $@

clean:
	rm ${BINS}
