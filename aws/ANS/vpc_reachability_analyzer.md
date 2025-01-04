# VPC features for Network Analysis

## VPC Reachability Analyzer vs. Network Access Analyzer

<br><img src="./img/amazon_vpc_dns_server_route53_resolver_img1.png" width="60%" /><br>

<br>

### VPC Reachability Analyzer

reachability analyzer는 트래픽의 시작점(source)과 목적지(destination)를 제공하면,
해당 시작점에서 목적지까지 연결이 되는지를 구분해줌

중간에 여러 홉(Hop)이 있을 수 있는데, (가령, internet Gateway, transit gateway 등)

이 홉에 Network-Access-Control-List security groups, VPC peering connections 등을 확인해보아야 함

<br>

### Network Access Analyzer

VPC Reachability Analyzer 과 비슷한 방식이지만, 시작점에서 목적지로 가는 '모든 경로'를 분석.

Compliant network 를 가지는 지 분석 가능

모든 트래픽이 방화벽, 혹은 특정 EC2, NAT Gateway 을 통하는지 확인할 때 사용할 수 있음

만약, 인터넷에 연결되면 안되는 서버 컴포넌트가 네트워크에 닿았다면, Network Access Analyzer를 보고 수정할 수 있음

<br>

# VPC Reachability Analyzer

**✔️ 1. Source resource 와 Destination resource 사이의 연결성(Connectivity) 테스트**

가령, Internet Gateway를 Source 로 지정하고, 특정 EC2를 Destination 으로 지정하고 VPC Reachability Analyzer 를 실행할 수 있음

<br><img src="./img/vpc_reachability_analyzer_img2.png" width="40%" /><br>

위 이미지에는 Internet Gateway 에서 Instance A로 가는 경로에는 🟥 Security Group 과 🟦 Network ACL 이 있음

추가로, Instance A에서 Internet Gateway 로 가는 경로도 생각해보아야 함

<br>

**✔️ 2. 가상 네트워크 경로의 각 홉마다 상세 정보 생성**

Source는 어떤 것이든 될 수 있음:

- Internet Gateway → Dest
- Internet Gateway 으로 향하는 NAT Gateway → Dest
- 동일 VPC에 위치한 다른 EC2 인스턴스 → Dest
- 다른 VPC에 위치한 다른 EC2 인스턴스 → Dest
- 등등

<br><img src="./img/vpc_reachability_analyzer_img3.png" width="40%" /><br>

<br>

**✔️ 3. 트래픽이 닿지 않을 때, 문제가 되는 컴포넌트를 정확히 짚어줌**

<br>

**✔️ 4. 테스트 시 실제 패킷을 전송하지 않음**

온전히 네트워크 설정을 사용해서 네트워크가 닿는지를 확인함

### Use cases

1. 네트워크를 잘못 설정해서 발생하는 연결성(Connectivity) 문제 트러블 슈팅
2. 네트워크 설정 변경 이후 연결성(Connectivity) 검증 자동화

<br>

### Supported Source & Destination

<br><img src="./img/vpc_reachability_analyzer_img4.png" width="80%" /><br>

<table>
<tr>
<th>Components</th>
<th>Intermediate components</th>
</tr>
<tr>
<td>

- Instance
- Internet Gateway
- Network Interfaces
- Transit Gateway
- Transit Gateway Attachments
- VPC endpoints
- VPC peering connections
- VPN gateways

</td>
<td>

- ALB and NLB
- NAT gateways
- TGW,TGW attachment, VPC peering

</td>
</tr>
</table>

<pre><b>Source 와 destination resources 위치:</b>
- 반드시 동일한 Region 내 위치
- 반드시 동일한 VPC 내 위치하거나, VPC Peering 혹은 Transit Gateway를 통해 연결되는 VPCs 내 위치
- 동일한 AWS Organization 내의 AWS account 사이에서 접근 가능
</pre>

<br><img src="./img/vpc_reachability_analyzer_img5.png" width="80%" /><br>

⚠️ IPv4 트래픽만을 지원함 (IPv6 지원 X)

<br>

### VPC Reachability Analyzer output

<br><img src="./img/vpc_reachability_analyzer_img6.png" width="80%" /><br>


---

## Exercise

#### Prerequisites

1. VPC 생성 (`vpc-0e8db6..`)
2. Internet Gateway
   - Name: demo 생성 (`igw-03386..`)
   - VPC 연결 (`igw-033860..` - attach - `vpc-0e8db6..`)
3. Subnet 생성 (`subnet-00fb2a..`)
   - in VPC (`vpc-0e8db6..`); VPC CIDR: `10.0.0.0/16`
   - Name: demo-subnet 생성
   - AZ: `ap-south-1a`
   - Subnet CIDR: `10.0.0.0/24`
4. Route Table (`rtb-0ab9c7..`)
   - Name: demo-rt
   - in VPC (`vpc-0e8db6..`)
   - 생성 후, Subnet Associations 에서 생성한 Subnet (`subnet-00fb2a..`) 연결

### 테스트 해볼 목록

- 1️⃣ Launch an EC2 instance in a **Private** subnet and access over SSH (`22`)
- 2️⃣ Modify the subnet route to make the subnet **Public**
- 3️⃣ Remove NACL inbound rule for inbound traffic
- 4️⃣ Add NACL inbound rule and remove Security group inbound rule to allow SSH (`22`) access

<br><img src="./img/vpc_reachability_analyzer_img7.png" width="80%" /><br>

<br><br>

### Exercise 1️⃣.

_Launch an EC2 instance in a **Private** subnet and access over SSH (`22`)_

<br>

#### 1.1. EC2 인스턴스 생성 (`i-0ad0f2..`)

- Key pair 사용
- 생성한 VPC (`vpc-0e8db6..`)와 Subnet (`subnet-00fb2a..`) 설정
- Public IP Enable 설정
- 자체 Security Group 생성 (Name: demo-sg, Open Anywhere)

생성한 EC2 의 Public IP 를 통해 SSH 연결을 시도

→ 의도한대로 연결되지 않음

아무리 Public IP를 Enable 설정했다고 해도, Private Subnet에 위치했기 때문

<br>

#### 1.2. Reachability Analyzer 생성 및 확인

연결이 안되는 것을 확인했으니, Reachability Analyzer로 트러블슈팅하는 과정을 확인. 

먼저 Reachability Analyzer을 설정해서 분석 생성

- Source Type: Internet Gateway
- Source: `igw-033860..`
- Destination Type: EC2 Instance
- Destination: `i-0ad0f2..`

생성 후 분석을 위해 10 ~ 15초 정도 분석 시간이 걸림

<br>

#### 1.3. Reachability 실패 확인

이후 Reachability status 에 **Not reachability** 표시가 되는 것을 확인할 수 있음

상세 내용을 확인하면, 의도한대로 Route Table `demo-rt`에 오류가 발생한 것을 확인할 수 있음

```
NO_ROUTE_TO_DESTINATION:
Route table rtb-0ab9c7.. does not have an applicable route to igw-033860..
```


<br>

### Exercise 2️⃣.

_Modify the subnet route to make the subnet **Public**_

<br>

#### 2.1. 오류 복구: Reachability 성공하도록 변경

Route table에 Internet Gateway 로 들어오는 트래픽을 허용

| Destination | Target                            | Status | Propagated |
|-------------|-----------------------------------|--------|------------|
| 0.0.0.0/0   | Internet Gateway; `igw-033860...` | -      | No         |

Route table 에 위의 행 추가

<br>

#### 2.2. Reachability 재검사 후 성공 확인

이후, `Analogize Path` 버튼을 눌러 다시 분석.

Reachability status가 **Reachable** 로 변경된 것을 알 수 있음


<br>

### Exercise 3️⃣.

_Remove NACL inbound rule for inbound traffic_

<br>

#### 3.1. NACL inbound rule 제거

demo-subnet 의 Network ACL에 가서 Inbound rules 확인

| Rule number | Type             | Protocol     | Port range | Source            | Allow/Deny   |
|-------------|------------------|--------------|------------|-------------------|--------------|
| **100**     | **All traffic**  | **All**      | **All**    | **0.0.0.0.0/0**   | **Allow**    |
| *           | All traffic      | All          | All        | 0.0.0.0/0/0       | Deny         |

위 처럼 설정된 Inbound rules에서 가장 첫 번째 줄 삭제:

| Rule number | Type             | Protocol     | Port range | Source            | Allow/Deny   |
|-------------|------------------|--------------|------------|-------------------|--------------|
| *           | All traffic      | All          | All        | 0.0.0.0/0/0       | Deny         |

<br>

#### 3.2. Reachability Analyzer가 NACL 오류를 뱉는지 검증

이후, `Analogize Path` 버튼을 눌러 다시 분석.

Reachability status 에 **Not reachability** 표시가 되는 것을 확인할 수 있음

상세 내용을 확인하면, 의도된 대로 Subnet 에 설정된 NACL에 아래와 같은 오류가 발생한 것을 확인할 수 있음

```
SUBNET_ACL_RESTRICTION:
Network ACL acl-0fd0688.. does not allow inbound traffic fron subnet-00fb2a.. to vpc-0e8db6..
```

<br>

#### 3.3. 오류 복구: Inbound Rule 다시 추가

다음 실습을 위해 동일한 Inbound Rule 다시 추가

이후, `Analogize Path` 버튼을 눌러 다시 분석하면 Reachability status가 **Reachable**로 표시됨


<br>

### Exercise 4️⃣.

_remove Security group inbound rule to allow SSH (`22`) access_

<br>

#### 4.1. Security Group inbound rule 제거

Security Group 에서 Inbound Rule 편집을 통해 기존에 정의된 규칙 제거

**before:**

| Security group rule ID | Type    | Protocol | Port range  | Source             |
|------------------------|---------|----------|-------------|--------------------|
| **dgr-0fd099..**       | **SSH** | **TCP**  | **22**      | **0.0.0.0.0/0**    |


**after:**

| Security group rule ID | Type | Protocol | Port range | Source |
|------------------------|------|----------|------------|--------|
| ( empty )              |      |          |            |        |


이후, `Analogize Path` 버튼을 눌러 다시 분석.

Reachability status 에 **Not reachability** 표시가 되는 것을 확인할 수 있음

상세 내용을 확인하면, 의도된 대로 ENI 부분에 오류가 발생한 것을 확인할 수 있음

```
ENI_SG_RULES_MISMATCH:
None of the ingress rules in the follow security groups apply
```


**마무리**

사용한 Resources 종료
