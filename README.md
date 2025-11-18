https://drive.google.com/drive/folders/18WadiD66FEvxRGxIxm_PUw5TyCHH_NAa

sudo dpkg --add-architecture i386
sudo mkdir -pm755 /etc/apt/keyrings
sudo wget -O /etc/apt/keyrings/winehq.key https://dl.winehq.org/wine-builds/winehq.key
echo "deb [signed-by=/etc/apt/keyrings/winehq.key] https://dl.winehq.org/wine-builds/ubuntu/ jammy main" | sudo tee /etc/apt/sources.list.d/winehq.list
sudo apt update
sudo apt install --install-recommends winehq-stable -y
