package com.example.spider.service;

import com.example.spider.controller.Util;
import com.example.spider.model.entities.Game;
import com.example.spider.model.entities.User;
import com.example.spider.model.exceptions.BadRequestException;
import com.example.spider.model.exceptions.NotFoundException;
import com.example.spider.model.repositories.GameRepository;
import com.example.spider.model.repositories.NGameRepository;
import com.example.spider.model.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AbstractService {

    @Autowired
    UserRepository userRepository;
    @Autowired
    GameRepository gameRepository;
    @Autowired
    NGameRepository nGameRepository;
    @Autowired
    ModelMapper mapper;
    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    public <T> T ifPresent(Optional<T> opt){
        if(!opt.isPresent()){
            throw new NotFoundException("Resource not found");
        }
        return opt.get();
    }

    public void validateWallet(final  User user, int stake, int numGames){
        if(user.getWallet()< (long) stake *numGames ){
            throw  new BadRequestException("Not enough money in your wallet");
        }
    }
    public void validatePeriod(LocalDate startDate, LocalDate endDate){
        if(startDate.isAfter(endDate)){
            throw new BadRequestException("Start date must be before end date");
        }
    }




    public Game generateGame(Game game, User u){
        game.setUser(u);
        game.setDateTimeGame(LocalDateTime.now());

        ArrayList<String> steps=getTrace();
        System.out.println("Total path is: " + steps);
        StringBuilder stringBuilder=new StringBuilder();
        for (int i = 0; i <steps.size() ; i++) {
            stringBuilder.append(steps.get(i));
            if(! (i==steps.size()-1)) {
                stringBuilder.append(",");
            }
        }
        String trace= stringBuilder.toString();
        game.setTrace(trace);
        game.setTotalWin((steps.size()-1)*Util.PROFIT_PER_STEPS);

        long outcome=game.getTotalWin()-game.getStake();
        game.setOutcome(outcome<0 ? Util.LOSS :Util.WIN);
        u.setWallet(u.getWallet()+outcome);
        return game;
    }

    public static ArrayList<String> getTrace(){

        ArrayList<String>stringArrayList=new ArrayList<>();
        Random r = new Random();
        stepFromStart(r,stringArrayList);
        return stringArrayList;
    }


    public enum START{B1,B2,B3}
    public enum B1{START,C1,C2}
    public enum B2{START,C1,C3}
    public enum B3{START,C2,C3}
    public enum C1{B1,B2,END}
    public enum C2{B1,B3,END}
    public enum C3{B2,B3,END}

    private static final Map<String, String[]> topConfig = new HashMap<>();
    static {
        topConfig.put("Start", new String[]{"B1", "B2", "B3"});
        topConfig.put("B1", new String[]{"Start","C1", "C3"});
        topConfig.put("B2", new String[]{"Start","C2", "C3"});
        topConfig.put("B3", new String[]{"Start","C1", "C2"});
        topConfig.put("C1", new String[]{"B1", "B2", "End"});
        topConfig.put("C2", new String[]{"B1", "B3", "End"});
        topConfig.put("C3", new String[]{"B2", "B3","End"});
    }
    public static void stepFromStart(Random random, ArrayList<String>path){
      String currentTop="Start";
      path.add(currentTop);
      moveSpider(random,path,currentTop);
    }

    private static void moveSpider(Random random, ArrayList<String> path, String currentTop) {
        if(topConfig.containsKey(currentTop)){
            String[]nextTops=topConfig.get(currentTop);
            int nextTopIdx=random.nextInt(nextTops.length);
            String cubeTop=nextTops[nextTopIdx];

            System.out.println("The spider goes from " + currentTop + " to " + cubeTop);
            path.add(cubeTop);

            if(! cubeTop.equals("End")){
                moveSpider(random,path,cubeTop);
            }
        }
    }
}
