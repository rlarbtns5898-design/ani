package com.example.demo.user.dto;

import java.util.List;

public class AnimeSearchCondition {

    private String keyword;
    private String type;
    private String status;
    private String rating;
    private Double minScore;
    private String startDate;
    private String endDate;

    private List<String> genres;
    private List<String> themes;
    private List<String> demographics;

    private String orderBy;
    private String sort;

    // getter & setter

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public Double getMinScore() { return minScore; }
    public void setMinScore(Double minScore) { this.minScore = minScore; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public List<String> getThemes() { return themes; }
    public void setThemes(List<String> themes) { this.themes = themes; }

    public List<String> getDemographics() { return demographics; }
    public void setDemographics(List<String> demographics) { this.demographics = demographics; }

    public String getOrderBy() { return orderBy; }
    public void setOrderBy(String orderBy) { this.orderBy = orderBy; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}