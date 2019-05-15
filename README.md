### LoLienBot
## 소개
* Clien League Of Legends (이하 롤) 소모임 LoLien당 전용 Discord Bot 입니다.
* 소모임 사람들과 롤을 할 때 편리한 기능들을 제공합니다.

## 명령어
* !소환사 등록 소환사명: 소환사를 Database에 등록합니다.
* !팀구성 밸런스 소환사명1, 소환사명2, 소환사명3 ...: 내전 팀 구성시 평균 티어를 맞추어 팀을 구성합니다.
* !내전 결과 조회: 최근 5개의 내전 이력을 보여줍니다.
* !내전 결과 등록 대전기록-URL 참가자목록(순서대로): 내전 후의 결과 데이터를 Database에 등록합니다. ([사진이 첨부된 자세한 설명][wiki])
* !내전 모스트 소환사명: 내전시 해당 소환사가 가장 많이 선택했던 챔프를 1위부터 5위까지 승률과 함께 보여줍니다.
* !내전 참여횟수: 내전 참여 횟수를 소환사명과 함께 1위부터 5위까지 보여줍니다.
* !내전 참여횟수 소환사명1, 소환사명2, 소환사명3 ...: 해당 소환사명의 내전 참여 횟수를 보여줍니다.

## 데이터 가시화 (차트)
![alt text](https://raw.githubusercontent.com/webgori/lolien-discord-bot/master/preview_chart.JPG)

아래 데이터를 [이곳][grafana]에서 차트로 보실 수 있습니다.
* 내전 현황
* 내전 최고 딜량 소환사 TOP 5
* 내전 최고 밴픽 챔피언 TOP 10


## 질문 또는 건의 사항
* 질문 또는 건의사항이 있으신 분은 GitHub Repository의 [Issues][issues] 메뉴를 이용해주시기 바랍니다.

## 라이선스
* ...

[wiki]: https://github.com/webgori/lolien-discord-bot/wiki/%EB%82%B4%EC%A0%84-%EA%B2%B0%EA%B3%BC-%EB%93%B1%EB%A1%9D-%EB%AA%85%EB%A0%B9%EC%96%B4
[grafana]: http://grafana.webgori.kr
[issues]: https://github.com/webgori/lolien-discord-bot/issues