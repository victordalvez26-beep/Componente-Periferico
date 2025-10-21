package uy.edu.tse.hcen.model;

import jakarta.persistence.*;

@Entity
public class Users {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
}
