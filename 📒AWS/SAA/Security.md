# AWS Security & Encryption

\# KMS \# Encryption SDK \# SSM Parameter Store

#### 1/ SSL: Encryption in flight (전송 중 암호화)

- 데이터를 **송신 전 암호화 + 수신 후 복호화**
- SSL Certificates: HTTPS - SSL 암호화 + SSL 복호화
- MITM (Man In The Middle Attack)의 안정성 보장

#### 2/ Server side Encryption at rest

- 데이터를 **서버에서 수신 후 암호화 + 전송 전 복호화**
- Data Key를 통해 암호화하여 저장
- Encryption/Decryption Key는 어딘가에(ie. KMS) 관리 및 저장 서버(ie. EBS)가 접근할 수 있어야 함

#### 3/ Client side Encryption

- 데이터를 **클라이언트가 전송 전 암호화 + 수신 후 복호화**
- 서버는 절대 데이터를 알 수 없음
- leverage Envelope Encryption: 봉투 암호화에 효과적


## 1. KMS

: AWS의 키 관리 서비스

- 권한 부여를 위해 IAM 완전히 통합되고
-  KMS로 암호화한 데이터에 관한 액세스를 더 쉽게 제어

- ⭐️ CloudTrail로 모든 KMS 키 호출 API를 감사할 수 있음

- 암호화가 필요한 거의 모든 서비스에서 KMS 통합을 활성화하여 사용 가능
- 암호 데이터는 절대로 평문으로 저장하면 안되고, (특히 코드 XXX)
- API 호출, AWS CLI, SDK을 통해 KMS 사용하여 코드나 환경변수에 저장하는 게 좋은 패턴
- new name of KMS custom master key



### KMS Keys 암호화 방식

**1. Symmetric (AES-256 keys), 대칭 키**
- 데이터 암호화와 복호화에 사용하는 단일 암호화 키
- KMS와 통합된 모든 AWS 서비스가 대칭 키를 사용
- KMS 대칭 키를 생성하거나 사용하면 키 자체에는 절대로 액세스할 수 없고 키를 활용 또는 사용하려면 KMS API를 호출해야 함

**2. Asymmetric (RSA & ECC key pairs), 비대칭 키**
- 2개의 키: 퍼블릭 키(암호화) → 프라이빗 키(복호화)
- 퍼블릭 키 다운로드 가능 / 프라이빗 키는 액세스 불가 (only API 호출)
- Use Case. KMS API 키에 액세스할 수 없는 사용자가 AWS 클라우드 외부에서 암호화할 때 사용


### 세가지 유형의 KMS Keys

| Key Type | Desc | Price | Automatic Key rotation |
|--|--|--|--|
| AWS Managed Key(AWS 관리형 키) | aws/*service-name* (aws/rds, aws/ebs, ...) | Free | 1년에 한 번 자동 교체 |
| Customer Managed Keys(CMK, 고객 관리형 키) | KMS를 사용해 생성 가능 | \$1/Month per a key | 활성화 필수, 1년에 한 번 자동 교체 |
| Customer Managed Keys imported | 반드시 256-bit의 대칭키이어야 함. 자체 키 구성 요소를 KMS에 가져옴 | \$1/Month per a key | 오직 수동으로 교체, 반드시 KMS 키 별칭 사용 |

**\+** 모든 KMS 호출 API 비용: $0.03 / 10,000건
**\+** 교체 빈도 변경 불가

이는 KMS 키로 암호화된 EBS 볼륨이 있고 리전은 eu-west-2라고 할 때 다른 리전으로 복사하려면 몇 가지 단계를 거쳐야 함을 의미

1. 암호화된 EBS 볼륨의 스냅샷을 생성
2. 생성된 암호화된 스냅샷을 동일한 KMS 키로 암호화
3. AWS가 자동으로 다른 KMS 키를 사용해서 스냅샷을 다시 암호화
4. KMS로 스냅샷을 자체 EBS 볼륨으로 복원
5. KMS Key B를 ap-southeast-2로 복원

\* 동일한 KMS 키가 서로 다른 리전에 있을 수는 없음


### KMS 키 정책

: KMS Key 액세스 제어

- S3 버킷 정책 비슷, but 정의된 KMS Key 정책이 없으면 누구도 액세스할 수 없음

2가지 유형의 KMS 키 정책

**Default KMS Key Policy, 기본 정책**
- 사용자 지정 키 정책을 제공하지 않으면 생성
- 계정의 모든 사람의 키 액세스 허용

**Custom KMS Key Policy, 사용자 지정 키 정책**
- KMS 키에 액세스할 수 있는 users 또는 roles을 정의 
- 키 관리자 정의
- 교차 계정 액세스 시 매우 유용: 다른 계정이 KMS 키를 사용하도록 권한을 부여하기 때문
  - 교차 계정 간에 스냅샷을 복사할 때 사용

**교차 계정 간 스냅샷 복사**
1. 자체 KMS 키로 암호화한 스냅샷을 생성 (고객 관리형 키: 고객 키 정책을 연결해야 함)
2. 교차 계정 액세스 권한 부여를 위해 KMS 키 정책을 연결
3. 암호화된 스냅샷을 대상 계정에 공유
4. 대상 계정에서는 스냅샷 복제본을 생성, 해당 대상 계정에서 다른 고객 관리형 키로 암호화
5. 대상 계정의 스냅샷에서 볼륨을 생성


### Practice

**STEP1.**
```bash
# 1) encryption
aws kms encrypt --key-id alias/tutorial --plaintext fileb://ExampleSecretFile.txt --output text --query CiphertextBlob  --region eu-west-2 > ExampleSecretFileEncrypted.base64
```

1. `--key-id alias/tutorial`: alias/tutorial이라는 key-id를 지정 (콘솔에서 생성한 키)
   -  Alias(별칭) / Serial Key Number(시리얼 키 번호) / ARN ... 모든 상관없음
2. `--plaintext fileb://ExampleSecretFile.txt`: 파일 주소를 평문(plaintext)으로 전달
3. `--output text`: 출력값은 암호화된 콘텐츠를 나타내는 CiphertextBlob을 텍스트(text) 그대로 입력
4. `--query CiphertextBlob`: 쿼리(query)는 암호화된 콘텐츠 명시
5. `--region eu-west-2`: 키의 리전(region)은 `eu-west-2` 로 지정
6. `ExampleSecretFileEncrypted.base64`: 암호화된 콘텐츠를 포함하는 base64 파일을 출력

**STEP2.** : Base64 decode

```bash
# base64 decode for Linux or Mac OS 
cat ExampleSecretFileEncrypted.base64 | base64 --decode > ExampleSecretFileEncrypted

# base64 decode for Windows
certutil -decode .\ExampleSecretFileEncrypted.base64 .\ExampleSecretFileEncrypted
```

**STEP3.** : KMS decryption

```bash
# 2) decryption

aws kms decrypt --ciphertext-blob fileb://ExampleSecretFileEncrypted   --output text --query Plaintext > ExampleFileDecrypted.base64  --region eu-west-2
```


**STEP3.** : Base64 decode

```bash
# base64 decode for Linux or Mac OS 
cat ExampleFileDecrypted.base64 | base64 --decode > ExampleFileDecrypted.txt

# base64 decode for Windows
certutil -decode .\ExampleFileDecrypted.base64 .\ExampleFileDecrypted.txt
```


## 2. KMS: Multi Region Key, 다중 리전 키


- 한 리전에 기본 키를 갖고 다른 리전에 키 구성 요소가 복제된 동일한 키를 가짐: 키 ID가 완전히 똑같음
  - *ex. us-east-1에 기본 키를 두고, 다른 리전 (ie. us-west-2, eu-west-1, ap-southeast-2)로 복제*

- ⭐️ **리전 간 교차 사용**: 한 리전에서 암호화한 후 다른 리전에서 복호화
  - ⚠️ KMS 다중 리전 키는 **전역 사용 불가**, **기본 키가 있고 복제본이 있는 것**
  - 다른 리전으로 복제할 때나 교차 리전 API 호출을 실행할 때, 데이터를 재암호화 필요 없음
- 동일한 키 ID와 동일한 키 구성 요소를 갖음
- 기본 키의 자동 교체를 활성화 시, 자동 교체 키가 다른 리전에도 복제됨

- KMS 키는 단일 리전에 제한되는 것을 선호: 특정 사례를 제외하고 다중 리전 키 사용을 권장하지 않음
  - 클라이언트 측 전역 암호화: 한 리전에서 클라이언트 측 암호화 -> 다른 리전에서 클라이언트 측 복호화
  - DynamoDB 전역 테이블 or Global Aurora 암호화


#### DynamoDB Global Tables / Aurora Global Tables + KMS Multi-Region Keys Client-Side Encryption

- ✔️ 전체 테이블뿐 아니라, 저장 데이터(테이블 속성)를 암호화
- 특정 클라이언트만 사용 가능: 데이터베이스 관리자도 사용 불가
- Amazon DynamoDB Encryption Client 사용

**Example.**

<img src="../img/kmsDynamoDbEncryption.png" />

1. us-east-1의 KMS 다중 리전 키(기본 키)를 ap-southeast-2 리전에 복제(복제본)
2. 클라이언트 애플리케이션에서 DynamoDB에 데이터를 삽입하려면 먼저 속성을 암호화
3. 다중 리전 키를 사용해 암호화할 속성을 암호화

<pre>
- 대부분의 DynamoDB 테이블 필드는 클라이언트 측 암호화가 필요없지만, 가령 사회 보장 번호는 암호화 필요
- DynamoDB 테이블에 액세스할 수 있는 DBA가 '사회 보장 번호' 속성을 암호화하는 데 사용한 KMS 키에 액세스할 수 있는 권한이 없다면 해당 데이터에 액세스할 수 없음
- 데이터베이스 관리자로부터도 보호 가능
</pre>

4. DynamoDB 테이블이 전역 테이블인 경우, 해당 테이블의 데이터는 ap-southeast-2 리전으로 복제
5. ap-southeast-2 리전의 클라이언트 애플리케이션은 데이터 중 암호화된 속성이 있는지 확인한 후, API 호출을 실행해 복제된 다중 리전 키를 사용해 해당 속성을 복호화 -> 다중 리전 키로 데이터 속성을 암호화하기로 했기 때문에 가능

**Benefits**
- Low-Latency API: 지연 시간이 단축
- 클라이언트 측 암호화 기술을 사용하면 데이터의 특정 필드나 속성을 보호
- API 키 액세스 권한이 있는 클라이언트만 복호화 가능


### S3 Replication Encryption Consideration

- S3는 암호화되지 않은 객체 & SSE-S3로 암호화된 객체들의 복제가 기본 제공

- SSE-C(고객 제공 키) 암호화 복제: 지원 X
  - 매번 키 제공 불가
- SSE-KMS
  - 옵션을 활성화 후 사용 가능
  - 어떤 KMS 키로 암호화할지 지정 필요
  - KMS 키 정책을 대상 키에 적용 필요
  - S3 복제 서비스를 허용하는 IAM 역할을 생성 -> 소스 버킷의 데이터를 먼저 복호화 -> KMS 키로 대상 버킷 데이터를 다시 암호화
  - 수많은 암호화와 복호화 발생: KMS 스로틀링 오류가 발생 가능 -> Service Quotas(서비스 할당) 요청

✔️ **다중 리전 키**로 **S3 복제**
- S3 복제에 다중 리전 키 사용 가능
- 하지만 S3 서비스 독립 키로 다루던 암/복호화는 지속
- 다중 리전 키도 동시에 암호화


### AMI Sharing Process Encrypted via KMS ⭐️⭐️⭐️⭐️⭐️

- 다른 계정과 AMI 공유

**Account A 계정의 AMI -> Account B 계정에 EC2 인스턴스 시작**

1. 소스 계정에 있는 AMI는 KMS 키로 암호화됨
2. AMI 속성 수정
  - Launch Permission(시작 권한) 추가: AMI가 시작하도록 허용하도록 B 계정 ID 추가
  - AMI 공유
3. B 계정으로 KMS 키 공유 (Key Policy, 키 정책)
4. B 계정에서 KMS 키와 AMI를 모두 사용할 수 있는 권한을 가진 IAM 역할이나 IAM 사용자를 생성: DescribeKey, ReEncrypted, CreateGrant, Decrypt API 호출에 대한 KMS 측 액세스 권한
5. AMI에서 EC2 인스턴스를 시작
  - (선택 사항) 대상 계정에서 볼륨을 재암호화하는 새로운 KMS 키로 볼륨 전체를 재암호화할 수 있음


## 3. SSM Parameter Store

- **SSM Parameter Store**: Systems Manager Parameter Store. 
- **구성**(Configuration) 및 **암호**(Secret)를 위한 보안 스토리지
- KMS 서비스를 이용한 암호화 지원
- 서버리스, 확장성, 내구성이 있고 SDK도 사용이 용이
- 매개변수를 업데이트 시, 구성 혹은 암호의 버전을 추적 가능
- IAM을 통한 보안 제공
- Amazon EventBridge 알림 수신 가능
- CloudFormation 통합 가능


### SSM Parameter Store Hierarchy

계층 구조가 있는 Parameter Store에 매개변수를 저장할 수 있음

<pre>
/my-department/
- my-app/
  ㄴ dev/
    ㄴ db-url
    ㄴ db-password
  ㄴ prod/
    ㄴ db-url
    ㄴ db-password
- other-app/
</pre>

- 구조화 -> IAM 정책을 간소화 가능 (*my-app/\** or */my-department/\**)
- Secrets Manager의 암호에 액세스할 수도 있음
- AWS에서 발행하는 퍼블릭 매개변수도 사용 가능 (ie. 특정 리전에서 Amazon Linux 2의 최신 AMI를 찾으려 할 때 Parameter Store에서 API 호출을 대신해 쓸 수 있음)

| | Standard | Advanced | 
|---|---|---|
| 생성 가능한 parameters 수 (per AWS account and Region) | 10,000 | 100,000 |
| Maximum size of a parameter value | **4 KB** | **8 KB** |
| Parameter policies available | No | Yes | 

**Storage Pricing**
| Standard | Advanced | Secret Manager |
|---|---|---|
 | Free | $0.05 per advanced parameter / a month | \$0.40 per secrets per month / \$0.05 per 10,000 API calls |


✔️ 고급 매개변수에서만 사용할 수 있는 매개변수 정책 - TTL(만료 기한)를 매개변수에 할당 가능
  - 민감한 정보를 업데이트 또는 삭제하도록 강제
  - 여러 정책을 할당 가능

**만료 정책 예시**

<table>
<tr>
  <th>Expiration</th>
  <th>ExpirationNotification</th>
  <th>NoChangeNotification</th>
</tr>
<tr>
<td><pre>{
    "Type": "Expiration",
    "Version": "1.0",
    "Attributes": {
        "Timestamp": "2018-12-02T21:34:33.000Z"
    }
}</pre>

- 타임스탬프의 시간이 되면 해당 매개변수를 반드시 삭제
</td>
  <td><pre>{
    "Type": "ExpirationNotification",
    "Version": "1.0",
    "Attributes": {
        "Before": "15",
        "Unit": "Days"
    }
}</pre>

- EventBridge와 통합함해서 EventBridge에서 알림을 받을 수 있음

- 매개변수가 만료되기 15일 전에 EventBridge 알림을 받음
</td>
<td><pre>{
    "Type": "NoChangeNotification",
    "Version": "1.0",
    "Attributes": {
        "After": "20",
        "Unit": "Days"
    }
}</pre>

- EventBridge가 변경이 없다는 알림 제공
</td>
</tr>
</table>


### Practice Section

```bash
# GET PARAMETERS / + DECRYPTION
aws ssm get-parameters --names /my-app/dev/db-url /my-app/dev/db-password
aws ssm get-parameters --names /my-app/dev/db-url /my-app/dev/db-password --with-decryption

# GET PARAMETERS BY PATH / + RECURSIVE / + DECRYPTION
aws ssm get-parameters-by-path --path /my-app/dev/
aws ssm get-parameters-by-path --path /my-app/ --recursive
aws ssm get-parameters-by-path --path /my-app/ --recursive --with-decryption
```

아래와 같이 SSM에서 Parameter를 가져올 수 있음 (복호화 후 조회 가능)  

```python
import json
import boto3
import os

ssm = boto3.client('ssm', region_name="eu-west-3")
dev_or_prod = os.environ['DEV_OR_PROD']

def lambda_handler(event, context):
    db_url = ssm.get_parameters(Names=["/my-app/" + dev_or_prod + "/db-url"])
    db_password = ssm.get_parameters(Names=["/my-app/" + dev_or_prod + "/db-password"], WithDecryption=True)
    return "worked!"
```


## 4. Secrets Manager

- 암호를 저장하는 최신 서비스
- N 일마다 암호 교체를 강제
- 새 암호를 생성할 Lambda 함수를 정의해서 교체할 암호를 강제 생성 및 자동화 가능
- AWS 서비스 통합: Amazon RDS, MySQL PostgreSQL, Aurora ...
  - 데이터베이스 접근 시 사용할 사용자 이름과 비밀번호를 저장 및 교체 가능
- KMS 서비스를 통해 암호화

(⭐️ **RDS와 Aurora의 통합 or 암호 -> AWS Secrets Manager**)

### Multi-Region Secrets

*다중 리전 암호*

- 복수 AWS 리전에 암호를 복제할 수 있고 기본 암호와 동기화된 읽기 전용 복제본을 유지한다는 개념
- 기본 리전에 암호를 하나 만들면 보조 리전에 동일한 암호가 복제된

**Why**
- 한 리전에 문제가 발생 시, 암호 복제본을 독립 실행형 암호로 승격할 수 있음
- 다중 리전 앱을 구축
- 재해 복구 전략도 짤 수 있음
- 다른 리전으로 복제되는 RDS 데이터베이스에 동일한 암호로 접근 가능

**Parameter Store 과의 차별점**
- Secret Manager는 교체, 관리 가능
- MySQL, PostgreSQL, Aurora, RDS 등 DB와 built-in integration(통합) 지원


## 5. AWS Certificate Manager (ACM)

*AWS Certificate Manager(ACM): TLS 인증서를 AWS에서 프로비저닝, 관리 및 배포*

```
오토 스케일링 그룹에 연결된 ALB가 있을 때, ALB를 HTTPS 엔드 포인트로서 노출, 
AWS Certificate Manager와 통합해 ALB에서 직접 TLS 인증서를 프로비저닝 및 유지 관리
```

- 사용자가 HTTPS 프로토콜을 사용하는 웹사이트 또는 API에 액세스
- ACM은 퍼블릭(무료)과 프라이빗 TLS 인증서를 모두 지원
- 인증서 자동 갱신
- 여러 AWS 서비스와 통합: 가령, 아래에서 TLS 인증서를 불러올 수 있음
  - 클래식 로드 밸런서(CLB) & Elastic Load Balancer(ELB) - ALB, NLB
  - CloudFront 배포
  - API Gateway의 모든 API
- BUT. EC2 인스턴스에서는 ACM을 사용할 수 없음 (퍼블릭 인증서일 경우 추출 불가능)

### 퍼블릭 인증서 요청 과정

1. 인증서에 포함할 도메인 이름을 나열 (도메인 수 제한 없음)
2. 유효성 검증 방법 선택: DNS 검증 or 이메일 검증
  - DNS 검증: SSL 인증서 자동화 목적 유용, DNS 구성에서 CNAME 레코드를 생성해 도메인 소유권을 증명해야 함 (Route 53이 있다면 ACM과 자동으로 통합)
3. 몇 시간 후 유효성 검증이 완료되면 인증서 발행
4. 퍼블릭 인증서도 자동 갱신 목록에 추가됨
   - ACM에서 모든 인증서를 만료 60일 전 자동 갱신

### Importing Public Certificates

- ACM 외부에서 생성된 인증서를 ACM으로 가져오는 옵션 제공
- 자동 갱신 불가능 (ACM 외부에서 생성되었기 때문)
- ACM 서비스가 만료 이벤트를 전송해 줌
  1. EventBridge, 만료 45일 전부터 매일 (기간 설정 가능) => Lambda 함수 or SNS topic or SQS 대기열
  2. AWS Config을 사용하는 방법
    - acm-certificate-expiration-check (만료된 인증서를 확인하는 관리형 규칙, 일수 조정 가능)을 설정해서 만료된 인증서가 있으면 이벤트가 EventBridge로 전송 => Lambda 함수 or SNS topic or SQS 대기열


이제 ACM이 API Gateway와 어떻게 통합되는지 살펴봅시다

<pre>
<h4>📌 API Gateway 엔드 포인트 유형</h4>
1. Edge-Optimized 
   - default, 엣지 최적화 유형
   - 글로벌 클라이언트를 위한 유형
   - 먼저 CloudFront 엣지 로케이션으로 요청을 라우팅하여 지연을 줄임
   - 한 리전에만 위치한 API Gateway로 보내는 경우

2. Regional
   - 리전 엔드 포인트 유형
   - 클라이언트가 API Gateway와 같은 리전에 있을 때
   - CloudFront는 사용할 수 없지만 자체 CloudFront 배포를 생성하여 캐싱 및 배포 전략을 제어할 수 있음

3. Private
   - 프라이빗 API Gateway 엔드포인트
   - 인터페이스 VPC 엔드 포인트(ENI)를 통해 VPC 내부에만 액세스할 수 있음
   - API Gateway에 대한 액세스를 정의하는 리소스 정책이 필요
</pre>

### ACM - API Gateway 통합

- ACM은 엣지 최적화(Edge-Optimized) 및 리전 엔드포인트(Regional)에 적합
- ACM을 API Gateway와 통합하려면 우선 API Gateway에 **사용자 지정 도메인 이름** 리소스를 생성하고 구성해야 함

**Edge-Optimized**
- 요청이 TLS 인증서가 연결된 CloudFront에서 라우팅
- TLS 인증서가 반드시 CloudFront와 같은 리전인 us-east-1에 생성되어야 함
- Route 53에서 CNAME이나 별칭(A-Alias) 레코드를 설정

**Regional**
- 리전이 같은 클라이언트를 위한 엔드포인트
- 같은 리전에 속한 API Gateway에 TLS 인증서가 포함되어야 함
- Route 53에서 CNAME이나 별칭 레코드가 목표 DNS를 가리키도록 설정


## 6. Web Application Firewall, WAF

*웹 애플리케이션 방화벽*

- AWS WAF는 Layer 7 (HTTP)에서 일반적인 웹 취약점 공격을 보호
- 웹 애플리케이션 방화벽(WAF) 적용
  - Application Load Balancer, API Gateway, CloudFront, AppSync GraphQL API, Cognito User Pool
  - NLB ❌

**웹 액세스 제어 목록(ACL) 규칙 정의**
- Filtering
  - IP 세트: IP 주소를 기반으로 필터링하는 등의 규칙, 최대 IP 주소 10,000개
  - HTTP headers 및 body, URI strings 보호 - SQL Injection -> 크로스 사이트 스크립팅(XSS) 등 공격 차단
  - 용량 제약 (ie. 최대 2MB)
  - 지역 일치(Geo-match) -> 특정 국가 허용 또는 차단
  - Rate-based 규칙: IP당 요청 수를 측정 -> 디도스 공격 차단 (ie. 특정 IP - 11 req per a sec)
- 웹 ACL은 리전에만 적용 (CloudFront는 글로벌로 정의)
- 규칙 그룹: 여러 웹 ACL에 추가할 수 있는 재사용 가능한 규칙 모음 (규칙 정리)


**WAF 유용한 사용 사례**
: 애플리케이션에 **고정 IP**를 사용하면서 **로드 밸런서**와 함께 **WAF**를 사용하고 싶을 때

- 애플리케이션 로드 밸런서는 고정 IP ❌ -> **AWS Global Accelerator**로 고정 IP 할당
- ALB에서 WAF를 활성화


## 7. AWS Shield - DDoS 보호

AWS Shield에 대해 알아봅시다

- 디도스 공격를 보호하기 위한 서비스

**AWS Shield Standard**
- 모든 AWS 고객에게 무료로 활성화되어 있는 서비스
- 고객을 SYN/UDP Floods, Reflection attacks 및 **L3/L4 공격**으로 부터 보호

**AWS Shield Advanced**

- (선택) 고급 보호가 필요한 고객을 위한 정교한 디도스 공격 완화 서비스
- 조직 당 월 3,000달러
- EC2, ELB, CloudFront, Global Accelerator, Route 53 등 보호
- DRP: DDoS response team, AWS 디도스 대응 팀이 항시 대기
- 디도스 공격으로 인한 요금 상승 방지
- 자동 애플리케이션 계층 디도스 완화를 지원하며, 자동으로 WAF 규칙을 생성, 평가, 배포함으로써 L7 공격을 완화.
  - 웹 애플리케이션 방화벽(WAF)이 L7 디도스 공격 완화 규칙을 자동으로 갖게 된다는 의미

## 8. Firewall Manager

- AWS Organization의 여러 계정의 방화벽 규칙을 동시에 관리하는 서비스
- 보안 정책(보안 규칙의 집합): 
  - 웹 애플리케이션 방화벽(WAF) (ALB, API Gateway CloudFront 등에 적용)
  - AWS Shield 어드밴스드 규칙 (ALB, CLB, NLB, Elastic IP, CloudFront)
  - 'EC2, ALB, VPC의 ENI 리소스'의 보안 그룹
  - VPC 수준의 AWS Network Firewall
  - Amazon Route 53 Resolver DNS Firewall
  - 정책은 리전 수준에서 생성
  

=> 모든 방화벽을 한 곳에서 관리할 수 있도록 지원, 조직에 등록된 모든 계정에 적용

> ✔️ 조직에서 애플리케이션 로드 밸런서에 대한 WAF 규칙을 생성한 다음 새 애플리케이션 로드 밸런서를 생성하는 경우 AWS Firewall Manager에서 **자동으로** 새 ALB에도 같은 규칙을 적용해 줌


### WAF vs. Firewall Manager vs. Shield

=> **모두 포괄적인 계정 보호를 위한 서비스**

**WAF**
- **when** 웹 ACL 규칙을 정의하는데 **리소스별 보호**를 구성할 때

**Firewall Manager**
- **when** 여러 계정에서 WAF를 사용 or WAF 구성을 가속 or 새 리소스 보호를 자동화를 구성할 때 (Firewall Manager로 WAF 규칙 관리)
- 모든 계정과 모든 리소스에 자동으로 적용
- Firewall Manager는 모든 계정에 Shield 어드밴스드를 배포에도 도움을 줌

**Shield 어드밴스드**
- WAF 기능 외 더 많은 기능 제공
  - Shield 대응 팀 지원 고급 보고서 제공
  - WAF 규칙 자동 생성
- 디도스 공격을 자주 받을 때


## 09. DDoS Protection Best Practices ⭐️

***DDoS 공격과 관련된 다양한 보호 방법을 알고 있어야 함***
- Edge Location Mitigation
- DDoS Mitigation
- Application Layer Defense
- Attack surface reduction

Q. Application Layer Defense는 공격을 받았을 때 스케일을 높이는 것으로 방지하는 것? 그럼 '방지'보다 '대책'에 더 가깝지 않나

**구성:** *EC2 인스턴스로 구성된 오토 스케일링 그룹이 있고 엘라스틱 로드 밸런서가 앞에 위치*

### Edge Location Mitigation

**✔️ Cloudfront**
- Edge Location에서 사용: 엣지 로케이션 완화
- SYN Flood나 UDP 반사 공격과 같은 DDoS 일반 공격은 Shield 설정으로 막을 수 있음
- CloudFront는 웹 애플리케이션 방화벽인 WAF 등과 연결할 수 있음 

<small>* AWS 의 CDN들의 여러 서비스들을 가장 빠른 속도로 제공(캐싱) 하기 위한 거점</small>

**✔️ Global Accelerator**

- 로드 밸런서를 Global Accelerator를 통하여 고정 IP로 노출하고 전 세계에서 애플리케이션에 액세스할 수 있음
- Global Accelerator는 Shield와 완전 통합
  - DDoS 공격 방어에 유용하게 쓰임
  - 백엔드가 CloudFront와 호환되지 않는 경우: 어떤 백엔드이던 간 CloudFront나 Global Accelerator로 AWS 엣지에 완전 분산이 가능 하며 엣지 로케이션을 DDoS 공격으로부터 보호할 수 있음

**✔️ Route 53**
- 엣지에 도메인 이름 변환을 글로벌로 설정
- DNS에도 DDoS 보호 메커니즘을 적용 가능
- 엣지에 대한 DDoS 보호를 더 확실히 할 수 있음


### DDoS Mitigation

**✔️ Infrastructure layer defense**

- EC2 인스턴스에 도달 전 높은 트래픽 관리
- Global Accelerator, Route 53, ALB은 높은 트래픽으로부터 Amazon EC2 인스턴스를 보호

**✔️ EC2 with Auto Scaling**

- 오토 스케일링 그룹이 자동으로 확장하여 더 큰 로드를 수용

**✔️ Elastic Load Balancing**
- ELB가 여러 EC2 인스턴스 간 트래픽을 자동으로 분산

### Application Layer Defense

**✔️ Detect and filter malicious web requests**
- 악성 요청을 감지 및 필터링
- **CloudFront**
  - 정적 콘텐츠 전송 시 엣지 로케이션에서 전송함으로써 백엔드를 보호
  - 특정 지역을 차단
- **WAF**
  - ALB나 CloudFront 전에 request signatures(요청 서명)에 따라 요청을 필터링 및 차단 가능
  - 특정 IP나 특정 요청 유형만 차단 가능
  - WAF의 rate-based rules(속도 기반 규칙): 악성 사용자의 IP를 자동으로 차단 가능
  - WAF의 관리형 규칙 - 평판에 따라 IP를 차단하거나 익명 IP 등을 차단할 수 있음


**✔️ Shield Advanced**

- 자동으로 WAF 규칙을 생성하여 계층 7 공격을 완화: 애플리케이션 계층 방어에 유용


### Attack surface reduction

**✔️ Obfuscating AWS resources (리소스 난독화)**
- CloudFront, API Gateway, ELB - 백엔드 리소스 숨기기

**✔️ 보안 그룹 & Network ACLs**
- 특정 IP의 트래픽을 필터링
- Elastic IP(탄력적 IP)도 AWS Shield Advanced로 보호할 수 있음

**✔️ API Endpoints**
- EC2, Lambda 등 어떤 백엔드든 숨겨 API 엔드 포인트 자체 보호
- Edge-optimized mode / CloudFront + Regional (for Global) -> DDoS 보호에 관한 제어 기능이 더 강화
- API Gateway + WAF: 모든 HTTP 요청을 필터링, 버스트 제한, 헤더 필터링, 사용자에게 API 키 사용 강제

## 11. Amazon GuardDuty

<img src="../img/guardDutyOverview.jpeg" width="600px" >

- AWS 계정을 보호를 위한 **지능형 위협 탐지 서비스**
- 머신 러닝 알고리즘을 사용하여 이상 탐지를 수행
- 타사 데이터를 이용하여 계정에 대한 공격을 탐지
- 여러 입력 데이터를 얻음
  - CloudTrail 이벤트 로그의 입력 데이터: 비정상적 API 호출과 무단 배포 등을 탐지
    - Management Event(관리 이벤트): VPC 서브넷을 만들 때, API가 계정에 호출 시
    - CloudTrail S3 데이터 이벤트 확인: get object, list objects delete object 등
  - VPC Flow Logs(흐름 로그): 비정상적인 인터넷 트래픽/IP 주소를 찾음
  - DNS 로그: DNS 쿼리에서 인코딩된 데이터를 전송할 EC2 인스턴스가 손상되었는지 확인 가능
  - Kubernetes 감사 로그를 확인: 의심스러운 활동 및 잠재적인 EKS 클러스터 손상을 감지
- CloudWatch 이벤트 규칙 설정: 탐색 결과가 나타나면 알림을 받을 수 있음
- ⭐️⭐️ **GuardDuty로 암호화폐 공격을 보호 - 전용 탐지 기능을 가짐**


## 12. Amazon Inspector

- 자동화된 보안 평가 서비스
- 실행 중인 EC2 Instances, ECR 컨테이너 이미지, Lambda 함수에서만 사용
  - Package Vulnerability: EC2, ECR, Lambda - database of CVE
  - Network Vulnerability: EC2
- 지속적인 수행
- 작업 완료 시 결과를 **AWS 보안 허브, Amazon Event Bridge**로 전송
취약성 데이터베이스, CVE

<small>*CVE(Common Vulnerabilities and Exposures): 알려진 컴퓨터 보안 결함 목록</small>


## 13. Amazon Macie

- 완전 관리형 데이터 보안 및 데이터 프라이버시 서비스

- **머신 러닝, 패턴 일치**를 사용하여 AWS의 민감한 정보를 검색하고 보호 (민감한 데이터를 경고)

- S3 버킷의 PII로 분류되는 데이터를 검색 -> CloudWatch 이벤트나 EventBridge로 검색 결과 전달

<small>*PII: Personal Identifiable Information</small>
