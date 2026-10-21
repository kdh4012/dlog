package com.dlog.watchlist;

import java.util.List;
import java.util.Map;

/** 시세 출처. StockQuoteService 가 우선순위(@Order) 순으로 시도한다 */
interface QuoteSource {

    /** 로그용 이름 */
    String name();

    /** 키 미설정 등으로 쓸 수 없으면 false */
    boolean isAvailable();

    /**
     * 티커 → 시세. 없는 종목은 결과에서 빠진다.
     *
     * @throws RuntimeException 외부 API 장애(네트워크 오류·5xx·호출 제한 등). 다음 출처로 넘어간다.
     */
    Map<String, StockQuote> fetch(List<String> tickers);
}
