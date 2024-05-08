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

