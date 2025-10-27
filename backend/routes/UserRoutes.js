import express from "express";
import UserController from "../controller/UserController.js";

const router = express.Router();

router.post("/send-code", UserController.sendCode);
router.post("/verify-code", UserController.verifyCode);
router.get("/verification-status/:telefono", UserController.getVerificationStatus);
router.post("/complete-register", UserController.completeRegistration);


export default router;