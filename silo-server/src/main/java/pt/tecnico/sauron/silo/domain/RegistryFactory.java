package pt.tecnico.sauron.silo.domain;

import java.util.Date;
import java.util.regex.Pattern;


import pt.tecnico.sauron.silo.domain.exceptions.IncorrectDataException;
import pt.tecnico.sauron.silo.domain.exceptions.InvalidTypeException;

public class RegistryFactory {

    private static final String CAR_ID_PATTERN = "([a-zA-Z]{2}\\d{2}[a-zA-Z]{2}|[a-zA-Z]{2}[a-zA-Z]{2}\\d{2}|\\d{2}[a-zA-Z]{2}[a-zA-Z]{2}|\\d{2}[a-zA-Z]{2}\\d{2}|\\d{2}\\d{2}[a-zA-Z]{2}|[a-zA-Z]{2}\\d{2}\\d{2})";
    private static final String PERSON_ID_PATTERN = "\\d+";

    public Registry build(CameraDomain cam, String type, String id, Date time) throws InvalidTypeException, IncorrectDataException {
        switch(type) {
            case "car":
                return buildCar(cam,type,id,time);
            case "person":
                return buildPerson(cam,type,id,time);   
            default:
                throw new InvalidTypeException(type);
        }
    }

    
    private Registry buildCar(CameraDomain cam, String type, String id, Date time) throws IncorrectDataException {
        //Validate id
        Pattern p = Pattern.compile(CAR_ID_PATTERN);
        if (!p.matcher(id).matches()) throw new IncorrectDataException(id, type);
        return new Registry(cam, type, id, time);
    }

    private Registry buildPerson(CameraDomain cam, String type, String id, Date time) throws IncorrectDataException {
        //Validate id
        Pattern p = Pattern.compile(PERSON_ID_PATTERN);
        if (!p.matcher(id).matches()) throw new IncorrectDataException(id, type);
        return new Registry(cam, type, id, time);
    }

}