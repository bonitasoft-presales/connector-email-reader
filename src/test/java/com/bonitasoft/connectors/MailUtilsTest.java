package com.bonitasoft.connectors;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class MailUtilsTest {

	@Test
	void shouldRemoveSignature() {
		String mail = "\n" +
				"Le sam. 31 oct. 2020 à 17:26, <presales.webdemo@bonitasoft.com> a écrit :\n" +
				"\n" +
				"> message\n" +
				"\n" +
				"\n" +
				"-- \n" +
				"\n" +
				"\n" +
				"\n" +
				"<https://bonitasoft.com>\n" +
				"<http://bonitasoft.com/>\n";

		String expected = "\n" +
				"Le sam. 31 oct. 2020 à 17:26, <presales.webdemo@bonitasoft.com> a écrit :\n" +
				"\n" +
				"> message\n" +
				"\n" +
				"\n";

		MailUtils mailUtils = new MailUtils();

		String sanitize = mailUtils.removeSignature(mail);

		assertThat(sanitize).isEqualTo(expected);

	}

	@Test
	void shouldInoreNoSignature() {
		String mail = "\n" +
				"Le sam. 31 oct. 2020 à 17:26, <presales.webdemo@bonitasoft.com> a écrit :\n" +
				"\n" +
				"> message\n" +
				"\n" +
				"\n" +
				"<https://bonitasoft.com>\n" +
				"<http://bonitasoft.com/>\n";


		MailUtils mailUtils = new MailUtils();

		String sanitize = mailUtils.removeSignature(mail);

		assertThat(sanitize).isEqualTo(mail);

	}

}