FROM ubuntu:20.04

ENV DEBIAN_FRONTEND=noninteractive
ENV LANG=C.UTF-8

# install required softwares
RUN apt update \
    && apt install -y wget curl git python vim unzip bzip2 xz-utils \
    python3 python3-pip \
    openjdk-8-jdk maven \
    openssh-client patch software-properties-common time build-essential \
    && rm -rf /var/lib/apt/lists/*

RUN wget https://github.com/robbyrussell/oh-my-zsh/raw/master/tools/install.sh -O - | zsh || true

# jdk7 downloaded from https://www.oracle.com/java/technologies/javase/javase7-archive-downloads.html
COPY jdk-7u80-linux-x64.tar.gz /tmp/jdk-7u80-linux-x64.tar.gz
RUN tar xvzf /tmp/jdk-7u80-linux-x64.tar.gz -C /tmp/
RUN mv /tmp/jdk1.7.0_80 /usr/lib/jvm/jdk1.7.0_80/
RUN rm /tmp/jdk-7u80-linux-x64.tar.gz

#########################################################
#########################################################
#########################################################

# install vul4j
COPY RepairThemAll/benchmarks/vul4j/ /vul4j/

WORKDIR /vul4j

RUN python3 setup.py install

# set env
ENV BENCHMARK_PATH /vul4j
ENV DATASET_PATH /vul4j/dataset/vul4j_dataset.csv
ENV REPRODUCTION_DIR /vul4j/reproduction
ENV PROJECT_REPOS_ROOT_PATH /vul4j/project_repos

RUN mkdir /repairthemall
RUN mkdir /workdir

# install RepairThemAll
# set env
ENV JAVA7_HOME /usr/lib/jvm/jdk1.7.0_80/bin
ENV JAVA8_HOME /usr/lib/jvm/java-8-openjdk-amd64/bin
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64
ENV MAVEN_BIN /usr/share/maven/bin

# working directory of repair processes in repairthemall
ENV WORKING_DIRECTORY /workdir

# to be mounted
ENV OUTPUT_PATH /results

COPY RepairThemAll/repair_tools /repairthemall/repair_tools
COPY RepairThemAll/libs /repairthemall/libs
COPY RepairThemAll/data /repairthemall/data
COPY RepairThemAll/init.sh /repairthemall/init.sh
COPY RepairThemAll/benchmarks /repairthemall/benchmarks
COPY RepairThemAll/script /repairthemall/script

WORKDIR /repairthemall

RUN /repairthemall/init.sh

# install SeqTrans
COPY SeqTrans/ /seqtrans

WORKDIR /seqtrans

RUN pip3 install -r requirements.txt

