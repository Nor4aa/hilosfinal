package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Question {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String enunciado;

	@Column(nullable = false)
	private String op1;
	@Column(nullable = false)
	private String op2;
	@Column(nullable = false)
	private String op3;
	@Column(nullable = false)
	private String op4;

	private int respuCorrect; // 1, 2, 3, or 4

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "block_id")
	private Block block;

	// Constructors
	public Question() {
	}

	public Question(String enunciado, String op1, String op2, String op3, String op4, int respuCorrect) {
		this.enunciado = enunciado;
		this.op1 = op1;
		this.op2 = op2;
		this.op3 = op3;
		this.op4 = op4;
		this.respuCorrect = respuCorrect;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEnunciado() {
		return enunciado;
	}

	public void setEnunciado(String enunciado) {
		this.enunciado = enunciado;
	}

	public String getOp1() {
		return op1;
	}

	public void setOp1(String op1) {
		this.op1 = op1;
	}

	public String getOp2() {
		return op2;
	}

	public void setOp2(String op2) {
		this.op2 = op2;
	}

	public String getOp3() {
		return op3;
	}

	public void setOp3(String op3) {
		this.op3 = op3;
	}

	public String getOp4() {
		return op4;
	}

	public void setOp4(String op4) {
		this.op4 = op4;
	}

	public int getRespuCorrect() {
		return respuCorrect;
	}

	public void setRespuCorrect(int respuCorrect) {
		this.respuCorrect = respuCorrect;
	}

	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}
}
