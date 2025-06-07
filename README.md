# Controle de Acesso com RFID, Arduino e Java

![maintained](https://img.shields.io/badge/maintained-no!%20(as%20of%202019)-red)
![PlatformIO CI](https://img.shields.io/badge/PlatformIO%20CI-passing-brightgreen)
![C++ 11](https://img.shields.io/badge/C++-11-blue)
![Java](https://img.shields.io/badge/Java-17%2B-orange)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-%3E%3D12-blue)
![release](https://img.shields.io/badge/release-v1.4.12-green)
![Arduino IDE](https://img.shields.io/badge/ArduinoIDE-%3E%3D1.6.10-lightgrey)

Este projeto demonstra um sistema completo de controle de acesso físico usando:

- **Arduino UNO + Módulo RFID RC522**
- **Java** como intermediário, fazendo a ponte entre hardware e banco de dados
- **PostgreSQL** para registro e validação dos cartões

## Funcionalidades

- **Cadastrar** cartões RFID
- **Validar acesso** (liberar ou negar)
- **Excluir** cartões cadastrados
- **Alternância de modo** por botão físico no Arduino
- **Feedback sonoro** (buzzer) e visual (LEDs)
- **Comunicação Serial Arduino <-> Java**

## Estrutura do projeto

arduino/ → Código Arduino (.ino)
java/src/ → Código Java (Main, Card, etc)
banco/ → Scripts SQL para criar a tabela no PostgreSQL
README.md → Este arquivo


## Como usar?

### 1. **Monte o circuito**
- Arduino UNO, Módulo RFID RC522, buzzer, LEDs, botão, jumpers
- Veja o diagrama no código Arduino

### 2. **Suba o código para o Arduino**
- Use o Arduino IDE para carregar o `.ino`

### 3. **Crie o banco de dados**
- Use o script `create_table.sql` no PostgreSQL

### 4. **Compile e rode o Java**
- Configure as dependências (ex: jSerialComm, PostgreSQL JDBC)
- Ajuste a porta COM e credenciais do banco no código

### 5. **Utilize!**
- Use o botão para alternar entre modos
- Veja feedback nos LEDs e buzzer ao apresentar cartões

## Créditos e inspiração

- Projeto criado por: João Vithor
- Inspirado em projetos open source de RFID/controle de acesso
- Referências: https://github.com/miguelbalboa/rfid

---

Sinta-se à vontade para tirar dúvidas!


**OBS**: ainda pretendo melhorar o projeto, então podem esperar mais atualizações
