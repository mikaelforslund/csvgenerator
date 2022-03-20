package com.example.demo;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.example.demo.csv.CsvGenerator;
import com.example.domain.Account;
import com.example.domain.AccountBalance;
import com.example.domain.AccountData;
import com.example.domain.CdrRequest;
import com.example.domain.DataItem;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
class MultipartdemoApplicationTests {

	@Test
	void contextLoads() {
	}

	private Pair<String, Stream<String[]>> generateCsv() {

		Account account1 = Account.builder()
			.accountData(AccountData.builder().productName("CREDIT CARD").build())
			.accountId("1")
			.dataItems(List.of(
				DataItem.builder().amount(100.0).description("This is a test 1").
				transactionId("1").build(),
				DataItem.builder().amount(50.0).description("This is a test 2")
				.transactionId("2").build()
			)).build();

		Account account2 = Account.builder()
			.accountData(AccountData.builder().productName("CREDIT CARD2")
			.accountBalance(List.of(AccountBalance.builder().balance(15.0).build(), AccountBalance.builder().balance(30.0).build())).build())
			.accountId("2")
			.dataItems(List.of(
					DataItem.builder().amount(100.0).description("This is a test 12")
							.transactionId("1").build(),
					DataItem.builder().amount(50.0).description("This is a test 22")
							.transactionId("2").build()))
			.build();

		CdrRequest request = CdrRequest.builder()
				.consent("CONSENT")
				.account(List.of(account1, account2))
				.build();

		List<String> header = List.of("consent", "productName", "accountId", "amount", "description", "transactionId", "balance");

		CsvGenerator csvGenerator = new CsvGenerator(header,"|", List.of(), Map.of("tags", (object, row, columnIndexMap) -> {
			//log.info("in column processor with object={} and row={}", object, row);
			return row;
		}));
		
		return Pair.of(csvGenerator.getHeader(), csvGenerator.toCsv(request));
	}

	@Test
	public void testSendMultipartStream() {

		StringBuilder sb = new StringBuilder();

		Pair<String, Stream<String[]>> rows = generateCsv();

		sb.append(rows.getLeft().concat("\n"));

		rows.getRight().forEach(row -> { 
			log.info("{}", Arrays.asList(row)); 
			sb.append(String.join("|", row).concat("\n"));
		});

		MultipartBodyBuilder builder = new MultipartBodyBuilder();
		builder.part("methods", "some methods");
		builder.part("metadata", Map.of("key", "value"));
		builder.part("file", new InputStreamResource(new ByteArrayInputStream(sb.toString().getBytes()))).filename("csv");

		String r = WebClient.builder().build()
			.post()
			.uri("http://localhost:8080/upload")
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.body(BodyInserters.fromMultipartData(builder.build()))
			.retrieve()
			.bodyToMono(String.class)
			.block();

		log.info("\nfrom server\n{}", r);
	}
}
