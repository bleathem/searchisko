package org.searchisko.ftest;

import com.jayway.restassured.http.ContentType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * @author Lukas Vlcek
 */
@RunWith(Arquillian.class)
public class AuthStatusRestServiceTest {

	@Deployment(testable = false)
	public static WebArchive createDeployment() {
		return DeploymentHelpers.createDeployment();
	}

	private final String restVersion = "v1/rest/";

	@Test
	@InSequence(0)
	public void assertSSOServiceNotAvailable(@ArquillianResource URL context) throws MalformedURLException {

		given().
				contentType(ContentType.JSON).
		expect().
				log().ifError().
//				log().ifStatusCodeIsEqualTo(200).
				contentType(ContentType.JSON).
				statusCode(200).
				body("authenticated", is(false)).
		when().
				get(new URL(context, restVersion + "auth/status").toExternalForm());
	}
}
