package com.cobnet.spring.boot.service;

import com.cobnet.common.KeyValuePair;
import com.cobnet.exception.ServiceDownException;
import com.cobnet.interfaces.spring.repository.StoreRepository;
import com.cobnet.spring.boot.core.ProjectBeanHolder;
import com.cobnet.spring.boot.dto.*;
import com.cobnet.spring.boot.dto.support.*;
import com.cobnet.spring.boot.entity.Store;
import com.cobnet.spring.boot.entity.support.Gender;
import com.google.maps.errors.ApiException;
import com.google.maps.errors.InvalidRequestException;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.PlaceAutocompleteType;
import com.google.maps.model.PlaceDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

@Service
public class StoreService {

    @Autowired
    private StoreRepository repository;

    public ResponseResult<GoogleApiRequestResultStatus> find(HttpServletRequest request, String name, AddressForm form) {

        return ProjectBeanHolder.getGoogleMapService().autocompleteRequest(request, PlaceAutocompleteType.ESTABLISHMENT, form, name);
    }

    public ResponseResult<GoogleApiRequestResultStatus> details(String storeId) {

        try {

            PlaceDetails details = ProjectBeanHolder.getGoogleMapService().search(storeId);

            if(details == null) {

                return new ResponseResult<>(GoogleApiRequestResultStatus.FAILED);
            }

            List<AddressComponent> components = Arrays.stream(ProjectBeanHolder.getGoogleMap().geocodingApiRequest().address(details.formattedAddress).await()).map(result -> result.addressComponents).flatMap(Arrays::stream).toList();

            StringBuilder street = new StringBuilder(), unit = new StringBuilder(), city = new StringBuilder(), state = new StringBuilder(), postalCode = new StringBuilder(), country = new StringBuilder();

            for(var component : components) {

                if(Arrays.stream(component.types).toList().contains(AddressComponentType.STREET_NUMBER)) {

                    street.insert(0, component.shortName);
                }

                if(Arrays.stream(component.types).toList().contains(AddressComponentType.ROUTE)) {

                    street.insert(street.length(), " ").insert(street.length(), component.shortName);
                }

                if(Arrays.stream(component.types).toList().contains(AddressComponentType.SUBPREMISE)) {

                    unit.append("Ste #").append(component.shortName.toUpperCase());
                }

                if(Arrays.stream(component.types).toList().containsAll(List.of(AddressComponentType.LOCALITY, AddressComponentType.POLITICAL))) {

                    city.append(component.shortName);
                }

                if(Arrays.stream(component.types).toList().containsAll(List.of(AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_1, AddressComponentType.POLITICAL))) {

                    state.append(component.shortName);
                }

                if(Arrays.stream(component.types).toList().containsAll(List.of(AddressComponentType.COUNTRY, AddressComponentType.POLITICAL))) {

                    country.append(component.longName);
                }

                if(Arrays.stream(component.types).toList().contains(AddressComponentType.POSTAL_CODE)) {

                    postalCode.append(component.shortName);
                }
            }

            return new ResponseResult<>(GoogleApiRequestResultStatus.SUCCESS,
                    new ObjectWrapper<>("name", details.name),
                    new AddressForm(street.toString(), unit.toString(), city.toString(), state.toString(), country.toString(), Integer.parseInt(postalCode.toString())),
                    new ObjectWrapper<>("is-permanently-closed", details.permanentlyClosed),
                    new ObjectWrapper<>("phone-number", details.internationalPhoneNumber),
                    new ObjectWrapper<>("rating", details.rating));

        } catch (IOException | InterruptedException | ApiException e) {

            e.printStackTrace();

            if(e.getCause() instanceof InvalidRequestException) {

            }

            return new ResponseResult<>(GoogleApiRequestResultStatus.SERVICE_DOWN);
        }
    }

    public ResponseResult<StoreRegisterResultStatus> register(StoreRegisterForm storeForm) {

        Store store = storeForm.getEntity();

        Optional<Store> existent = repository.findById(store.getId());

        if(existent.isPresent()) {

            return new ResponseResult<>(StoreRegisterResultStatus.STORE_REGISTERED);
        }

        ResponseResult<GoogleApiRequestResultStatus> details = this.details(store.getId());

        if(((ObjectWrapper<Boolean>)details.contents()[2]).getValue()) {

            return new ResponseResult<>(StoreRegisterResultStatus.STORE_PERMANENTLY_CLOSED);
        }

        if(details.status() == GoogleApiRequestResultStatus.SERVICE_DOWN) {

            return new ResponseResult<>(StoreRegisterResultStatus.SERVICE_DOWN);
        }

        if(details.contents().length == 0) {

            return new ResponseResult<>(StoreRegisterResultStatus.STORE_NONEXISTENT);
        }

        if(details.status() == GoogleApiRequestResultStatus.SUCCESS) {

            store.setName(((ObjectWrapper<String>) (details.contents()[0])).getValue());
            store.setLocation(((AddressForm) details.contents()[1]).getEntity());
            store.setPhone(((ObjectWrapper<String>) details.contents()[3]).getValue());

            repository.save(store);

            return new ResponseResult<>(StoreRegisterResultStatus.SUCCESS);
        }
System.out.println(details.status());
        return new ResponseResult<>(StoreRegisterResultStatus.SERVICE_DOWN);
    }

    public ResponseResult<StoreCheckInPageDetailResultStatus> getStoreCheckInPageDetail(String storeId, Locale locale) throws IOException {

        Optional<Store> store = repository.findById(storeId);

//        if(store.isEmpty()) {
//
//            return new ResponseResult<>(StoreCheckInPageDetailResultStatus.NO_EXIST);
//        }

        try {

            List<String> referralOptions = new ArrayList<>();

            for(SurveyReferralOption option : SurveyReferralOption.values()) {

                referralOptions.add(ProjectBeanHolder.getTranslatorMessageSource().getMessage(option.getKey(), locale));
            }

            return new ResponseResult<>(StoreCheckInPageDetailResultStatus.SUCCESS, new DynamicPage(new DynamicPageProperties(),
                new StepContainerPageField(0, "firstName", ProjectBeanHolder.getTranslatorMessageSource().getMessage("label.first-name", locale), PageFieldType.INPUT, new DynamicPageFieldProperties()),
                new StepContainerPageField(0, "lastName", ProjectBeanHolder.getTranslatorMessageSource().getMessage("label.last-name", locale), PageFieldType.INPUT, new DynamicPageFieldProperties()),
                new StepContainerPageField(1, "gender", ProjectBeanHolder.getTranslatorMessageSource().getMessage("label.gender", locale), PageFieldType.RADIO, new DynamicPageFieldProperties(new KeyValuePair<>("list", Gender.values()))),
                new StepContainerPageField(2, "phoneNumber", ProjectBeanHolder.getTranslatorMessageSource().getMessage("label.phone-number", locale), PageFieldType.INPUT, new DynamicPageFieldProperties()),
                new StepContainerPageField(3, "referral", ProjectBeanHolder.getTranslatorMessageSource().getMessage("label.referral", locale), PageFieldType.RADIO, new DynamicPageFieldProperties(new KeyValuePair<>("list", referralOptions)))
            ));

        } catch (ServiceDownException ex) {

            return new ResponseResult<>(StoreCheckInPageDetailResultStatus.SERVICE_DOWN);
        }

    }
}
