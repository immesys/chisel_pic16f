// look in pins.pcf for all the pin names on the TinyFPGA BX board
module top (
    input CLK,    // 16MHz clock
    inout [13:1] PINSL,
    inout [24:17] PINSR,
    input RST,
    input ECLK,
    output USBPU  // USB pull-up resistor
);

  wire [13:1] pinsl_out_en;
  wire [13:1] pinsl_out;
  wire [13:1] pinsl_in;

  wire [24:17] pinsr_out_en;
  wire [24:17] pinsr_out;
  wire [24:17] pinsr_in;

  wire pin_eclk;

  SB_IO
  #(
      .PIN_TYPE(6'b 1010_01),
      .PULLUP(1'b 1)
  )
  IO_PINSL [13:1]
  (
  .PACKAGE_PIN (PINSL), // User’s Pin signal name
  .OUTPUT_ENABLE (pinsl_out_en), // Output Pin Tristate/Enable
  .D_OUT_0 (pinsl_out), // Data 0 – out to Pin/Rising clk
  .D_IN_0 (pinsl_in), // Data 0 - Pin input/Rising clk
  ) /* synthesis DRIVE_STRENGTH = x2 */  ;

  SB_IO
  #(
      .PIN_TYPE(6'b 1010_01),
      .PULLUP(1'b 1)
  )
  IO_PINSR[24:17]
  (
  .PACKAGE_PIN (PINSR), // User’s Pin signal name
  .OUTPUT_ENABLE (pinsr_out_en), // Output Pin Tristate/Enable
  .D_OUT_0 (pinsr_out), // Data 0 – out to Pin/Rising clk
  .D_IN_0 (pinsr_in), // Data 0 - Pin input/Rising clk
  ) /* synthesis DRIVE_STRENGTH = x2 */  ;

  SB_GB_IO
  #(
      .PIN_TYPE(6'b 0000_01),
      .PULLUP(1'b 1)
  )
  IO_ECLK
  (
  .PACKAGE_PIN (ECLK),
  .GLOBAL_BUFFER_OUTPUT (pin_eclk)
  ) ;


  Toplevel gatepc (
    .clock    ( CLK    ),
    .reset    ( RST    ),

    .io_ebus_in   ({pinsl_in[13:1], pinsr_in[24:22]}),
    .io_ebus_out  ({pinsl_out[13:1], pinsr_out[24:22]}),
    .io_ebus_en   ({pinsl_out_en[13:1], pinsr_out_en[24:22]}),
    .io_ebus_alatch  (pinsr_out[21]),
    .io_ebus_read    (pinsr_out[20]),
    .io_ebus_write   (pinsr_out[19]),
  );

  assign pinsr_out_en[21:19] = 3'b 111;
  assign pinsr_out_en[18:17] = 2'b 00;

  // drive USB pull-up resistor to '0' to disable USB
  assign USBPU = 0;

endmodule
