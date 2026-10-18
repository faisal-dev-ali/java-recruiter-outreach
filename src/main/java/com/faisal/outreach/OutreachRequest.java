package com.faisal.outreach;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OutreachRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String recruiterName;
    private String recruiterEmail;
    private String companyName;
}