package uk.tw.energy.controller;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.tw.energy.service.AccountService;
import uk.tw.energy.service.PricePlanService;

@RestController
@RequestMapping("/calculateCost")
public class ConsumptionCostController {

	private final AccountService accountService;
	private final PricePlanService pricePlanService;

	public ConsumptionCostController(AccountService accountService,PricePlanService pricePlanService) {
		this.accountService = accountService;
		this.pricePlanService = pricePlanService;
	}

	@GetMapping("/weeklyUsageCost/{smartMeterId}")
	public ResponseEntity getWeeklyUsageCost(@PathVariable String smartMeterId) {
		String pricePlanName = accountService.getPricePlanIdForSmartMeterId(smartMeterId);
		BigDecimal consumptionsForPricePlans =
				pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlanForWeek(smartMeterId,pricePlanName);

		return ResponseEntity.ok(consumptionsForPricePlans);				
	}
}
