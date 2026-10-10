ARG BUILD_FROM
FROM $BUILD_FROM

RUN apk add --no-cache \
    git \
    cmake \
    make \
    g++ \
    python3 \
    ffmpeg \
    curl \
    bash

WORKDIR /opt
RUN git clone --depth 1 https://github.com/ggerganov/whisper.cpp.git

WORKDIR /opt/whisper.cpp
RUN cmake -B build -DCMAKE_BUILD_TYPE=Release \
    && cmake --build build --config Release -j"$(nproc)" --target whisper-cli

COPY run.sh /run.sh
COPY server.py /server.py
RUN chmod a+x /run.sh

CMD [ "/run.sh" ]
