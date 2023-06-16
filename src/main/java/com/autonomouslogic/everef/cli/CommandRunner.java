package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.cli.decorator.HealthcheckDecorator;
import com.autonomouslogic.everef.cli.decorator.SlackDecorator;
import com.autonomouslogic.everef.cli.markethistory.ScrapeMarketHistory;
import com.autonomouslogic.everef.cli.marketorders.ScrapeMarketOrders;
import com.autonomouslogic.everef.cli.publiccontracts.ScrapePublicContracts;
import com.autonomouslogic.everef.cli.refdata.BuildRefData;
import io.reactivex.rxjava3.core.Completable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class CommandRunner {
	@Inject
	protected Provider<Placeholder> placeholderProvider;

	@Inject
	protected Provider<DataIndex> dataIndexProvider;

	@Inject
	protected Provider<ScrapeMarketOrders> scrapeMarketOrdersProvider;

	@Inject
	protected Provider<ScrapePublicContracts> scrapePublicContractsProvider;

	@Inject
	protected Provider<BuildRefData> buildRefDataProvider;

	@Inject
	protected Provider<PublishRefData> publishRefDataProvider;

	@Inject
	protected Provider<ScrapeMarketHistory> scrapeMarketHistoryProvider;

	@Inject
	protected Provider<VerifyRefDataModels> verifyRefDataModelsProvider;

	@Inject
	protected Provider<ScrapeHoboleaks> scrapeHoboleaksProvider;

	@Inject
	protected Provider<ImportTestResources> importTestResourcesProvider;

	@Inject
	protected HealthcheckDecorator healthcheckDecorator;

	@Inject
	protected SlackDecorator slackDecorator;

	@Inject
	protected CommandRunner() {}

	public Completable runCommand(String[] args) {
		if (args.length == 0) {
			throw new IllegalArgumentException("No command specified");
		}
		if (args.length > 1) {
			throw new IllegalArgumentException("More than one command specified");
		}
		var command = createCommand(args[0]);
		command = decorateCommand(command);
		return runCommand(command);
	}

	private Completable runCommand(Command command) {
		return Completable.defer(() -> {
			var name = command.getName();
			log.info(String.format("Executing command: %s", name));
			var start = Instant.now();
			return command.run().andThen(Completable.fromAction(() -> {
				var time = Duration.between(start, Instant.now());
				log.info(String.format("Command %s completed in %s", name, time.truncatedTo(ChronoUnit.MILLIS)));
			}));
		});
	}

	private Command createCommand(String name) {
		switch (name) {
			case "placeholder":
				return placeholderProvider.get();
			case "data-index":
				return dataIndexProvider.get();
			case "scrape-market-orders":
				return scrapeMarketOrdersProvider.get();
			case "scrape-public-contracts":
				return scrapePublicContractsProvider.get();
			case "build-ref-data":
				return buildRefDataProvider.get();
			case "publish-ref-data":
				return publishRefDataProvider.get();
			case "scrape-market-history":
				return scrapeMarketHistoryProvider.get();
			case "verify-ref-data-models":
				return verifyRefDataModelsProvider.get();
			case "scrape-hoboleaks":
				return scrapeHoboleaksProvider.get();
			case "import-test-resources":
				return importTestResourcesProvider.get();
			default:
				throw new IllegalArgumentException("Unknown command: " + name);
		}
	}

	private Command decorateCommand(Command command) {
		command = healthcheckDecorator.decorate(command);
		command = slackDecorator.decorate(command);
		return command;
	}
}
