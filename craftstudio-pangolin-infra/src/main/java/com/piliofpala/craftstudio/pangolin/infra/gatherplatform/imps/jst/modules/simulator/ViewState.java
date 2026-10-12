package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator;

public class ViewState {
    private String viewState;
    private String viewStateGenerator;

    public ViewState(String viewState, String viewStateGenerator) {
        this.viewState = viewState;
        this.viewStateGenerator = viewStateGenerator;
    }

    public String getViewState() {
        return viewState;
    }

    public void setViewState(String viewState) {
        this.viewState = viewState;
    }

    public String getViewStateGenerator() {
        return viewStateGenerator;
    }

    public void setViewStateGenerator(String viewStateGenerator) {
        this.viewStateGenerator = viewStateGenerator;
    }

    public String dumpInfo() {
        return "__VIEWSTATE: "+viewState+"\n"+
                "__VIEWSTATEGENERATOR: "+viewStateGenerator;
    }
}
