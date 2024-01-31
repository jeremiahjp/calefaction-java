package com.jp.calefaction.service.urbandictionary;

import com.austinv11.servicer.Service;
import com.jp.calefaction.model.urbandictionary.UrbanDictionaryResponse;
import java.util.Collections;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UrbanDictionarySorter {
    public static UrbanDictionaryResponse sortEntriesByPopularity(UrbanDictionaryResponse response) {
        log.info("Working on sorting... hopefully this doesnt take too long");
        if (response != null && response.getList() != null) {
            Collections.sort(response.getList(), new Comparator<UrbanDictionaryResponse.Entry>() {
                @Override
                public int compare(UrbanDictionaryResponse.Entry entry1, UrbanDictionaryResponse.Entry entry2) {
                    return Integer.compare(entry2.getThumbs_up(), entry1.getThumbs_up());
                }
            });
        }
        return response;
    }
}
