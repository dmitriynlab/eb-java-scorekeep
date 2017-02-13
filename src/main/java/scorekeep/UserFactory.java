package scorekeep;
import java.util.*;
import java.security.SecureRandom;
import java.math.BigInteger;
import java.lang.Exception;
import java.io.InputStream;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactory;
import com.amazonaws.regions.Regions;

public class UserFactory {
  private SecureRandom random = new SecureRandom();
  private UserModel model = new UserModel();
  private AWSLambda lambdaClient = AWSLambdaClientBuilder.standard()
        .withRegion(Regions.fromName(System.getenv("AWS_REGION")))
        .build();

  public UserFactory(){
  }

  public User newUser() throws IOException {
    String id = new BigInteger(40, random).toString(32).toUpperCase();
    User user = new User(id);
    String category = "American names";
    String name = randomNameLambda(id, category);
    user.setName(name);
    model.saveUser(user);
    return user;
  }

  public User newUser(String name) throws IOException {
    String id = new BigInteger(40, random).toString(32).toUpperCase();
    User user = new User(id);
    user.setName(name);
    model.saveUser(user);
    return user;
  }

  public String randomName() throws IOException {
    CloseableHttpClient httpclient = HttpClientBuilder.create().build();
    HttpGet httpGet = new HttpGet("http://uinames.com/api/");
    CloseableHttpResponse response = httpclient.execute(httpGet);
    try {
      HttpEntity entity = response.getEntity();
      InputStream inputStream = entity.getContent();
      ObjectMapper mapper = new ObjectMapper();
      Map<String, String> jsonMap = mapper.readValue(inputStream, Map.class);
      String name = jsonMap.get("name");
      EntityUtils.consume(entity);
      return name;
    } finally {
      response.close();
    }
  }

  public String randomNameLambda(String userid, String category) throws IOException {
    RandomNameService service = LambdaInvokerFactory.build(RandomNameService.class, lambdaClient);
    RandomNameInput input = new RandomNameInput();
    input.setCategory(category);
    input.setUserid(userid);
    /** If this fails, state is set but get state fails*/
    String name = service.randomName(input).getName();
    return name;
  }

  public User getUser(String userId) throws UserNotFoundException {
    return model.loadUser(userId);
  }

  public List<User> getUsers() {
    return model.loadUsers();
  }
}