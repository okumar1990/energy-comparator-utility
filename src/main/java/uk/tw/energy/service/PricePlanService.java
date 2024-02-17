package uk.tw.energy.service;

import org.springframework.format.datetime.joda.MillisecondInstantPrinter;
import org.springframework.stereotype.Service;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PricePlanService {

    private final List<PricePlan> pricePlans;
    private final MeterReadingService meterReadingService;

    public PricePlanService(List<PricePlan> pricePlans, MeterReadingService meterReadingService) {
        this.pricePlans = pricePlans;
        this.meterReadingService = meterReadingService;
    }

    public Optional<Map<String, BigDecimal>> getConsumptionCostOfElectricityReadingsForEachPricePlan(String smartMeterId) {
        Optional<List<ElectricityReading>> electricityReadings = meterReadingService.getReadings(smartMeterId);

        if (!electricityReadings.isPresent()) {
            return Optional.empty();
        }

        return Optional.of(pricePlans.stream().collect(
                Collectors.toMap(PricePlan::getPlanName, t -> calculateCost(electricityReadings.get(), t))));
    }
    
    public BigDecimal getConsumptionCostOfElectricityReadingsForEachPricePlanForWeek(String smartMeterId,String pricePlanName) {
        Optional<List<ElectricityReading>> electricityReadings = meterReadingService.getReadings(smartMeterId);

        if (!electricityReadings.isPresent()) {
            return BigDecimal.ZERO;
        }
        PricePlan pricePlan = pricePlans.stream().filter(x->x.getPlanName().equals(pricePlanName)).findAny().get();
        return calculateCostForAWeek(electricityReadings.get(),pricePlan);
    }

    private BigDecimal calculateCost(List<ElectricityReading> electricityReadings, PricePlan pricePlan) {
        BigDecimal average = calculateAverageReading(electricityReadings);
        BigDecimal timeElapsed = calculateTimeElapsed(electricityReadings);

        BigDecimal averagedCost = average.divide(timeElapsed, RoundingMode.HALF_UP);
        return averagedCost.multiply(pricePlan.getUnitRate());
    }
    
    private BigDecimal calculateCostForAWeek(List<ElectricityReading> electricityReadings, PricePlan pricePlan) {
    	electricityReadings = electricityReadings.stream().filter(x->x.getTime().compareTo(Instant.now().minus(7, ChronoUnit.DAYS))==0).collect(Collectors.toList());
    	return calculateCost(electricityReadings,pricePlan);
    }
    
    private BigDecimal calculateAverageReading(List<ElectricityReading> electricityReadings) {
        BigDecimal summedReadings = electricityReadings.stream()
                .map(ElectricityReading::getReading)
                .reduce(BigDecimal.ZERO, (reading, accumulator) -> reading.add(accumulator));

        return summedReadings.divide(BigDecimal.valueOf(electricityReadings.size()), RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTimeElapsed(List<ElectricityReading> electricityReadings) {
        ElectricityReading first = electricityReadings.stream()
                .min(Comparator.comparing(ElectricityReading::getTime))
                .get();
        ElectricityReading last = electricityReadings.stream()
                .max(Comparator.comparing(ElectricityReading::getTime))
                .get();

        return BigDecimal.valueOf(Duration.between(first.getTime(), last.getTime()).getSeconds() / 3600.0);
    }
    
    private BigDecimal calculateWeeklyTimeElapsed(List<ElectricityReading> electricityReadings) {
         //List<ElectricityReading> weeklyReadings = electricityReadings.stream().filter(x->x.getTime() > Instant.now().minusMillis(7*24*60*60*1000)).collect(Collectors.toList());
        //ElectricityReading first = 
        ElectricityReading last = electricityReadings.stream()
                .max(Comparator.comparing(ElectricityReading::getTime))
                .get();

        return BigDecimal.valueOf(Duration.between(Instant.now().minusMillis(7*24*60*60*1000), Instant.now()).getSeconds() / 3600.0);
    }

}
