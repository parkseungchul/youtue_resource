https://drive.google.com/drive/folders/18WadiD66FEvxRGxIxm_PUw5TyCHH_NAa

sudo dpkg --add-architecture i386


sudo mkdir -pm755 /etc/apt/keyrings


sudo wget -O /etc/apt/keyrings/winehq.key https://dl.winehq.org/wine-builds/winehq.key


echo "deb [signed-by=/etc/apt/keyrings/winehq.key] https://dl.winehq.org/wine-builds/ubuntu/ jammy main" | sudo tee /etc/apt/sources.list.d/winehq.list



sudo apt update



sudo apt install --install-recommends winehq-stable -y



sudo rm -f /etc/apt/sources.list.d/winehq*.*
sudo rm -f /etc/apt/sources.list.d/*wine*.*
sudo rm -f /etc/apt/sources.list.d/*.sources
sudo rm -f /etc/apt/sources.list.d/*.list




1️⃣ WineHQ 저장소 파일 모두 삭제
(이 디렉토리 둘 중에 어디든)* 남은 파일 다 지워야 함*

sudo rm -f /etc/apt/sources.list.d/winehq*.*

sudo rm -f /etc/apt/sources.list.d/*wine*.*

sudo rm -f /etc/apt/sources.list.d/*.sources

sudo rm -f /etc/apt/sources.list.d/*.list


2️⃣ /etc/apt/sources.list 내부에 Wine 라인이 있는지 확인

파일 열기:


sudo vi /etc/apt/sources.list



안에 이런 라인 있으면 무조건 삭제:

deb https://dl.winehq.org/wine-builds/ubuntu jammy main


삭제 후 저장:

저장: Ctrl + O

종료: Ctrl + X

3️⃣ 키 파일 재확인 (혹시 남았을 경우)

sudo rm -f /etc/apt/keyrings/winehq*

sudo rm -f /etc/apt/keyrings/*wine*


4️⃣ 이제 apt 개청소 업데이트
sudo apt update





