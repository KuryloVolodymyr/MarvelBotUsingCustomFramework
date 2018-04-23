package com;

import com.marvelDTO.MarvelCharacterResponse;
import com.marvelDTO.MarvelComicsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDate;

public class ApiCaller {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${marvel.privateKey}")
    private String marvelPrivateKey;

    @Value("${marvel.publicKey}")
    private String marvelPublicKey;

    @Value("${marvel.characterUrl}")
    private String marvelCharacterUrl;

    @Value("${marvel.comicsUrl}")
    private String marvelComicsUrl;

    @Value("${marvel.recentComicsUrl}")
    private String marvelRecentComicsUrl;

    @Value("${marvel.characterUrlStartsWith}")
    private String marvelCharacterStartsWith;

    public MarvelCharacterResponse callMarvelForCharacter(String characterName) {
        String limit = "3";

        String ts = new Timestamp(System.currentTimeMillis()).toString();

        String hash = createHash(ts);

        MarvelCharacterResponse singleCharacter = restTemplate.getForObject(marvelCharacterUrl, MarvelCharacterResponse.class, characterName, limit, ts, marvelPublicKey, hash);

        if (!singleCharacter.getData().getResults().isEmpty()) {
            return singleCharacter;
        } else {
            ts = new Timestamp(System.currentTimeMillis()).toString();

            hash = createHash(ts);

            return restTemplate.getForObject(marvelCharacterStartsWith, MarvelCharacterResponse.class, characterName, limit, ts, marvelPublicKey, hash);

        }
    }

    public MarvelComicsResponse callMarvelForComics(String characterId) {
        String limit = "5";

        String ts = new Timestamp(System.currentTimeMillis()).toString();

        String hash = createHash(ts);

        return restTemplate.getForObject(marvelComicsUrl, MarvelComicsResponse.class, characterId, limit, ts, marvelPublicKey, hash);
    }

    public MarvelComicsResponse callMarvelApiForRecentComics(String characterId, String previousRequestDate) {
        String limit = "5";

        String ts = new Timestamp(System.currentTimeMillis()).toString();

        String hash = createHash(ts);

        String currentDate = LocalDate.now().toString();

        System.out.println(previousRequestDate);
        System.out.println(currentDate);

        return restTemplate.getForObject(marvelRecentComicsUrl, MarvelComicsResponse.class, characterId,
                previousRequestDate, currentDate, ts, marvelPublicKey, hash);

    }

    private String createHash(String ts) {
        String hash = "hash";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String toHash = ts + marvelPrivateKey + marvelPublicKey;
            md.update(toHash.getBytes());
            byte byteData[] = md.digest();

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            hash = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("MD5 Exception");
        }
        return hash;
    }

}
