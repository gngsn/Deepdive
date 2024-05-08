### **Encryption**

사용자가 서버로 안전하게 데이터를 전송하는 방법은?

**1. Plain Text Format**

```
             🥷🏼
👩🏻‍💻  ---[ Plain Text ]-->  💾
```

Hacker Sniffing 취약

=> Encrypt Plain Text


2. Encrypted

```
               🥷🏼 📄
👩🏻‍💻  ---[ 📄 Encrypted Text ]-->  💾
```

하지만 서버도 암호화된 내용을 모르기 때문에 암호화 키 공유 필요

=> **Symmetric Encryption**

<br>

### **Symmetric Encryption**

유저가 키를 생성해서 서버에 전달하면, 암호화한 데이터를 서로 복호화해서 사용할 수 있음 

```
 👩🏻‍  ----[ 🗝️ ]--->  💾
 🗝️
```

`(..)🔒` 은 `🗝️` 로 암호화한 데이터

```
 👩🏻‍  ----[ (📄)🔒]--->  💾
 🗝️                  🗝️
```

안전하게 전송 성공

```
 👩🏻‍              💾
 🗝️           ️📄 🗝
```

이 아니라, 중간에서 키를 가로챌 수 있음

```
FAIL
        🥷🏼 📄 🗝️
👩🏻‍  ----[ 🗝️ ]--->  💾
```

→ Hacker Sniffing !



<br>

### **Asymmetric Encryption**

#### SSH

**Public Key 🔒  + Private Key 🔑**

오직 같이 발급된 쌍으로만 암호화/복호화 할 수 있음

1. 키 생성

```
          👩🏻‍
     $ ssh-keygen
→ 🔒 🔑 (id_rsa, id_rsa.pub)
```

2. 모든 서버 접근을 막고, 오직 Public Key 로 보호한 접근만 오픈

```
           +-----------------+
👩🏻‍         🔒   💾 server     |
🔒 🔑       +-----------------+
```

서버: `cat ~/.ssh/authorized_keys`

3. private key 로만 서버 접근

```Bash
ssh -i id_rsa user@server1
```

**❓ 여러 서버가 필요하면?**

따로 발급한 Key와 Lock 사용

```
             +---------------+
             [🔒1]  server   |
             [🔒2]    💾     |
 👩🏻 [🔑1]    +---------------+
 🧑🏻‍ [🔑2]    +---------------+
             [🔒1]  server   |
             [🔒2]    💾     |
             +---------------+
```

문제점: 너무 많은 연산으로 서버/클라이언트가 모두 부담

<br>

---

### **Symmetric Encryption in a Safe Way**


Private Key와 Public Key를 생성하기 위한 명령어는 조금 다름

**1. 서버 키 페어 생성**

```
           🥷🏼
   👩🏻‍💻              💾
   🗝️              🔒 🔑
Symmetric 
   Key
```

`🔑` 는 서버가 생성한 Private Key (개인키)

`🔒` 는 서버가 생성한 Public Key (공개키)

`🗝️` 는 사용자가 생성한 Symmetric Key (대칭키)

<br>

명령어가 다름

```Bash
$ openssl genrsa -out my-bank.key 1024
$ openssl rsa -in my-bank.key -oubout > mybank.pem
$ ls
my-bank.key mybank.pem
```

<br>

**2. 서버 Public Key 유저에게 전달**

```
            🥷🏼 🔒
   👩🏻‍💻 <----[🔒]-----  💾
   🗝️                 🔑
Symmetric 
   Key
```

암호화된 대칭키를 서버로 전달

```
          Hacker's Hand
           🔒 & (🗝️)🔒
              🥷🏼
   👩🏻‍💻  -----[(🗝️)🔒]--->  💾
  (🗝️)🔒                🔑 (🗝️)🔒 → 🔑🗝️
Encrypted
Symmetric 
   Key
```

` (🗝️)🔒`는 서버의 공개키(`🔒`)로 암호화한 비대칭키 (`🗝️`)


이제 유저가 해당 Symmetric Key로 암호화해서 보내도 서버에서 복호화 가능


```
          🥷🏼 🔒(🗝️)🔒(📄️)
          nothing to do
   👩🏻‍💻  -----[(📄️)]--->  💾
  (📄️)(🗝)🔒         (📄️) 🔑🗝️ → 📄️🗝️
Encrypted
Symmetric 
   Key
```
