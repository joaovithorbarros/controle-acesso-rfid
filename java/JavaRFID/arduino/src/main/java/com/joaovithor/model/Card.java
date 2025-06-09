package com.joaovithor.model;

public class Card {
    private Long id;
    private String UID;
    private String date;
    
    public Card(){}

    public Card(String UID, String date){
        this.UID = UID;
        this.date = date;
    }

    public Long getId(){
        return id;
    }

    public String getUID(){
        return UID;
    }

    public String getDate(){
        return date;
    }

    public void setId(Long id){
        if(id != null){
            return;
        }
        this.id = id;
    }

    public void setUID(String UID){
        if(UID != null){
            this.UID = UID;
        }
    }

    public void setDate(String date){
        this.date = date;
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj) return true;
        if(!(obj instanceof Card)) return false;
        Card other = (Card) obj;
        return this.UID.equalsIgnoreCase(other.UID);
    }

    @Override
    public String toString(){
        return "Card{UID='" + UID + "', date='"+ date + "'}";
    }
}
