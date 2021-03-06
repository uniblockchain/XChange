package org.knowm.xchange.anx.v2.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;

import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.anx.ANXUtils;
import org.knowm.xchange.anx.v2.ANXAdapters;
import org.knowm.xchange.anx.v2.ANXExchange;
import org.knowm.xchange.anx.v2.dto.trade.ANXTradeResultWrapper;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.dto.trade.OpenOrders;
import org.knowm.xchange.dto.trade.UserTrades;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.service.trade.TradeService;
import org.knowm.xchange.service.trade.params.DefaultTradeHistoryParamsTimeSpan;
import org.knowm.xchange.service.trade.params.TradeHistoryParams;
import org.knowm.xchange.service.trade.params.TradeHistoryParamsTimeSpan;
import org.knowm.xchange.service.trade.params.orders.OpenOrdersParams;
import org.knowm.xchange.utils.Assert;
import org.knowm.xchange.utils.DateUtils;

/**
 * @author timmolter
 */
public class ANXTradeService extends ANXTradeServiceRaw implements TradeService {

  /**
   * Constructor
   *
   * @param baseExchange
   */
  public ANXTradeService(BaseExchange baseExchange) {

    super(baseExchange);
  }

  @Override
  public OpenOrders getOpenOrders() throws IOException {
    return getOpenOrders(createOpenOrdersParams());
  }

  @Override
  public OpenOrders getOpenOrders(
      OpenOrdersParams params) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
    return new OpenOrders(ANXAdapters.adaptOrders(getANXOpenOrders()));
  }

  @Override
  public String placeMarketOrder(MarketOrder marketOrder) throws IOException {

    return placeANXMarketOrder(marketOrder).getDataString();
  }

  @Override
  public String placeLimitOrder(LimitOrder limitOrder) throws IOException {

    // Validation
    Assert.notNull(limitOrder.getLimitPrice(), "getLimitPrice() cannot be null");
    Assert.notNull(limitOrder.getTradableAmount(), "getTradableAmount() cannot be null");

    if (limitOrder.getTradableAmount().scale() > 8) {
      throw new IllegalArgumentException("tradableAmount scale exceeds max");
    }

    if (limitOrder.getLimitPrice().scale() > ANXUtils.getMaxPriceScale(limitOrder.getCurrencyPair())) {
      throw new IllegalArgumentException("price scale exceeds max");
    }

    String type = limitOrder.getType().equals(OrderType.BID) ? "bid" : "ask";

    BigDecimal amount = limitOrder.getTradableAmount();
    BigDecimal price = limitOrder.getLimitPrice();

    return placeANXLimitOrder(limitOrder.getCurrencyPair(), type, amount, price).getDataString();
  }

  @Override
  public boolean cancelOrder(String orderId) throws IOException {

    Assert.notNull(orderId, "orderId cannot be null");

    return cancelANXOrder(orderId, "BTC", "EUR").getResult().equals("success");
  }

  private UserTrades getTradeHistory(Long from, Long to) throws IOException {
    ANXTradeResultWrapper rawTrades = getExecutedANXTrades(from, to);
    String error = rawTrades.getError();

    if (error != null) {
      throw new IllegalStateException(error);
    }

    return ANXAdapters.adaptUserTrades(rawTrades.getAnxTradeResults(), ((ANXExchange) exchange).getANXMetaData());
  }

  /**
   * Supported parameter types: {@link TradeHistoryParamsTimeSpan}
   */
  @Override
  public UserTrades getTradeHistory(TradeHistoryParams params) throws ExchangeException, IOException {

    Long from = null;
    Long to = null;
    if (params instanceof TradeHistoryParamsTimeSpan) {
      TradeHistoryParamsTimeSpan p = (TradeHistoryParamsTimeSpan) params;
      from = DateUtils.toMillisNullSafe(p.getStartTime());
      to = DateUtils.toMillisNullSafe(p.getEndTime());
    }
    return getTradeHistory(from, to);
  }

  @Override
  public TradeHistoryParams createTradeHistoryParams() {

    return new DefaultTradeHistoryParamsTimeSpan();
  }

  @Override
  public OpenOrdersParams createOpenOrdersParams() {
    return null;
  }

  @Override
  public Collection<Order> getOrder(
      String... orderIds) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
    throw new NotYetImplementedForExchangeException();
  }

}
