#include <SPI.h>
#include <MFRC522.h>


#define SS_PIN 10
#define RST_PIN 9
#define BUZZER_PIN 2
#define BUTTON_PIN 4

MFRC522 rfid(SS_PIN, RST_PIN);

bool ultimoEstado = HIGH;
int modo = 0; // 0 a 2 -> 0 : CADASTRO | 1 : VALIDAÇÃO | 2 : DELETAR
unsigned long debounce = 0;

void setup() {
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(BUTTON_PIN, INPUT_PULLUP);
  digitalWrite(BUZZER_PIN, LOW);
  Serial.begin(9600);
  SPI.begin();
  rfid.PCD_Init();
  Serial.println("MODO:CADASTRAR");

}
//BOTÃO | SOM
void bip(int tempo = 150, int frq = 2000){
  tone(BUZZER_PIN, frq, tempo);
  delay(tempo + 10);
  noTone(BUZZER_PIN);
}

void bipPermitido(){ bip(200, 2000); }
void bipNegado()   { bip(500, 300);  }

void loop() {
  if (Serial.available()) {
    String comando = Serial.readStringUntil('\n');
    comando.trim();

    if (comando == "BIP:PERMITIDO") {
      bipPermitido();
    } else if (comando == "BIP:NEGADO") {
      bipNegado();
    }
  }
  
  //BOTÃO | LÓGICA
  bool estadoAtual = digitalRead(BUTTON_PIN);
  if(estadoAtual == HIGH && !ultimoEstado && millis() - debounce > 300){
    modo = (modo + 1) % 3;
    debounce = millis();
    if (modo == 0) Serial.println("MODO:CADASTRAR");
    else if (modo == 1) Serial.println("MODO:VALIDAR");
    else if (modo == 2) Serial.println("MODO:DELETAR");
  }
  ultimoEstado = estadoAtual;
  if (!rfid.PICC_IsNewCardPresent() || !rfid.PICC_ReadCardSerial()) return;

  bip();
  
  Serial.print("UID: ");
  for(byte i = 0; i < 4; i++){
    Serial.print(rfid.uid.uidByte[i] < 0x10 ? "0" : "");
    Serial.print(rfid.uid.uidByte[i], HEX);
  }

  Serial.println();

  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();
  delay(1000);

}
