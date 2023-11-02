/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.creation;

import org.junit.Test;
import org.mockito.exceptions.base.MockitoException;
import org.mockitoutil.TestBase;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MockSettingsImplTest extends TestBase {

    private MockSettingsImpl mockSettingsImpl = new MockSettingsImpl();

    @Test(expected=MockitoException.class)
    public void shouldNotAllowSettingNullInterface() {
        mockSettingsImpl.extraInterfaces(List.class, null);
    }
    
    @Test(expected=MockitoException.class)
    public void shouldNotAllowNonInterfaces() {
        mockSettingsImpl.extraInterfaces(List.class, LinkedList.class);
    }
    
    @Test(expected=MockitoException.class)
    public void shouldNotAllowUsingTheSameInterfaceAsExtra() {
        mockSettingsImpl.extraInterfaces(List.class, LinkedList.class);
    }
    
    @Test(expected=MockitoException.class)
    public void shouldNotAllowEmptyExtraInterfaces() {
        mockSettingsImpl.extraInterfaces();
    }
    
    @Test(expected=MockitoException.class)
    public void shouldNotAllowNullArrayOfExtraInterfaces() {
        mockSettingsImpl.extraInterfaces((Class[]) null);
    }
    
    @Test
    public void shouldAllowMultipleInterfaces() {
        //when
        mockSettingsImpl.extraInterfaces(List.class, Set.class);
        
        //then
        assertEquals(List.class, mockSettingsImpl.getExtraInterfaces()[0]);
        assertEquals(Set.class, mockSettingsImpl.getExtraInterfaces()[1]);
    }

    @Test
    public void shouldSetMockToBeSerializable() throws Exception {
        //when
        mockSettingsImpl.serializable();

        //then
        assertTrue(mockSettingsImpl.isSerializable());
    }

    @Test
    public void shouldKnowIfIsSerializable() throws Exception {
        //given
        assertFalse(mockSettingsImpl.isSerializable());

        //when
        mockSettingsImpl.serializable();

        //then
        assertTrue(mockSettingsImpl.isSerializable());
    }
}