package gov.samhsa.c2s.c2suiapi.service;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import feign.FeignException;
import gov.samhsa.c2s.c2suiapi.infrastructure.PhrClient;
import gov.samhsa.c2s.c2suiapi.service.exception.NoDocumentsFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PhrServiceImpl implements PhrService{

    private final PhrClient phrClient;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Autowired
    public PhrServiceImpl(PhrClient phrClient) {
        this.phrClient = phrClient;
    }

    @Override
    public List<Object> getAllDocumentTypeCodesList(){
        return phrClient.getAllDocumentTypeCodesList();
    }

    @Override
    public List<Object> getPatientDocumentInfoList(String patientMrn){
        try{
            return phrClient.getPatientDocumentInfoList(patientMrn);
        }catch(HystrixRuntimeException err) {
            Throwable t = err.getCause();
            if(t instanceof FeignException && ((FeignException) t).status() == 404){
                throw new NoDocumentsFoundException(t.getMessage());
            }
        }
        return null;
    }

}
