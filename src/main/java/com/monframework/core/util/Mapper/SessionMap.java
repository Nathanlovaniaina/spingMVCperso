package com.monframework.core.util.Mapper;

import jakarta.servlet.http.HttpSession;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Enumeration;

/**
 * Wrapper pour manipuler la HttpSession comme une Map<String, Object>.
 * Cette classe permet d'ajouter, récupérer, modifier et supprimer des attributs de session
 * en utilisant l'interface Map standard.
 */
public class SessionMap implements Map<String, Object> {
    
    private final HttpSession session;
    
    public SessionMap(HttpSession session) {
        if (session == null) {
            throw new IllegalArgumentException("HttpSession ne peut pas être null");
        }
        this.session = session;
    }
    
    @Override
    public int size() {
        int count = 0;
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            names.nextElement();
            count++;
        }
        return count;
    }
    
    @Override
    public boolean isEmpty() {
        return !session.getAttributeNames().hasMoreElements();
    }
    
    @Override
    public boolean containsKey(Object key) {
        if (key == null || !(key instanceof String)) {
            return false;
        }
        return session.getAttribute((String) key) != null;
    }
    
    @Override
    public boolean containsValue(Object value) {
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            Object attrValue = session.getAttribute(names.nextElement());
            if (value == null ? attrValue == null : value.equals(attrValue)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public Object get(Object key) {
        if (key == null || !(key instanceof String)) {
            return null;
        }
        return session.getAttribute((String) key);
    }
    
    @Override
    public Object put(String key, Object value) {
        if (key == null) {
            throw new NullPointerException("La clé ne peut pas être null");
        }
        Object oldValue = session.getAttribute(key);
        session.setAttribute(key, value);
        return oldValue;
    }
    
    @Override
    public Object remove(Object key) {
        if (key == null || !(key instanceof String)) {
            return null;
        }
        Object oldValue = session.getAttribute((String) key);
        session.removeAttribute((String) key);
        return oldValue;
    }
    
    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        if (m == null) {
            throw new NullPointerException("La map ne peut pas être null");
        }
        for (Entry<? extends String, ? extends Object> entry : m.entrySet()) {
            session.setAttribute(entry.getKey(), entry.getValue());
        }
    }
    
    @Override
    public void clear() {
        Enumeration<String> names = session.getAttributeNames();
        java.util.List<String> namesList = new java.util.ArrayList<>();
        while (names.hasMoreElements()) {
            namesList.add(names.nextElement());
        }
        for (String name : namesList) {
            session.removeAttribute(name);
        }
    }
    
    @Override
    public Set<String> keySet() {
        Set<String> keys = new java.util.HashSet<>();
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            keys.add(names.nextElement());
        }
        return keys;
    }
    
    @Override
    public Collection<Object> values() {
        Collection<Object> values = new java.util.ArrayList<>();
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            values.add(session.getAttribute(names.nextElement()));
        }
        return values;
    }
    
    @Override
    public Set<Entry<String, Object>> entrySet() {
        Set<Entry<String, Object>> entries = new java.util.HashSet<>();
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            Object value = session.getAttribute(name);
            entries.add(new SimpleEntry<>(name, value));
        }
        return entries;
    }
    
    /**
     * Classe interne pour représenter une entrée de la Map.
     */
    private static class SimpleEntry<K, V> implements Entry<K, V> {
        private final K key;
        private V value;
        
        public SimpleEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }
        
        @Override
        public K getKey() {
            return key;
        }
        
        @Override
        public V getValue() {
            return value;
        }
        
        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<?, ?> e = (Entry<?, ?>) o;
            return (key == null ? e.getKey() == null : key.equals(e.getKey()))
                && (value == null ? e.getValue() == null : value.equals(e.getValue()));
        }
        
        @Override
        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) ^
                   (value == null ? 0 : value.hashCode());
        }
    }
}
