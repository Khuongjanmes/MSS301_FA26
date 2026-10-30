/* Creates MSS301_2026_PE with the rooms and reservations tables from
   create_rooms_sql.txt / create_reservations_sql.txt, plus sample data.
   Run: sqlcmd -S localhost -E -i setup_database.sql */
USE [master];
GO

IF DB_ID(N'MSS301_2026_PE') IS NULL
BEGIN
    EXEC(N'CREATE DATABASE [MSS301_2026_PE]');
END;
GO

USE [MSS301_2026_PE];
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID(N'dbo.rooms', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[rooms] (
        [room_id]         INT              IDENTITY(1,1) NOT NULL,
        [room_number]     NVARCHAR(20)     NOT NULL,
        [room_type]       NVARCHAR(10)     NOT NULL,
        [price_per_night] DECIMAL(18,2)    NOT NULL,
        [capacity]        INT              NOT NULL,
        [floor]           INT              NOT NULL,
        [status]          NVARCHAR(15)     NOT NULL,

        CONSTRAINT [PK_Rooms] PRIMARY KEY CLUSTERED ([room_id] ASC),
        CONSTRAINT [UQ_Rooms_Number] UNIQUE NONCLUSTERED ([room_number] ASC),
        CONSTRAINT [CK_Rooms_Type]
            CHECK ([room_type] IN ('SINGLE', 'DOUBLE', 'SUITE', 'DELUXE')),
        CONSTRAINT [CK_Rooms_Price]
            CHECK ([price_per_night] > 0),
        CONSTRAINT [CK_Rooms_Capacity]
            CHECK ([capacity] >= 1 AND [capacity] <= 10),
        CONSTRAINT [CK_Rooms_Floor]
            CHECK ([floor] >= 1),
        CONSTRAINT [CK_Rooms_Status]
            CHECK ([status] IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE'))
    );
END;
GO

IF OBJECT_ID(N'dbo.reservations', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[reservations](
        [reservation_id]   [int]           IDENTITY(1,1) NOT NULL,
        [guest_name]       [nvarchar](100) NOT NULL,
        [guest_email]      [nvarchar](100) NOT NULL,
        [guest_phone]      [nvarchar](20)  NOT NULL,
        [check_in_date]    [date]          NOT NULL,
        [check_out_date]   [date]          NOT NULL,
        [number_of_guests] [int]           NOT NULL,
        [total_amount]     [decimal](18,2) NOT NULL,
        [status]           [nvarchar](15)  NOT NULL,
        [room_id]          [int]           NOT NULL,

        CONSTRAINT [PK_Reservations] PRIMARY KEY CLUSTERED ([reservation_id] ASC),
        CONSTRAINT [CK_Reservations_Dates]
            CHECK ([check_out_date] > [check_in_date]),
        CONSTRAINT [CK_Reservations_Guests]
            CHECK ([number_of_guests] >= 1),
        CONSTRAINT [CK_Reservations_Amount]
            CHECK ([total_amount] >= 0),
        CONSTRAINT [CK_Reservations_Status]
            CHECK ([status] IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')),
        CONSTRAINT [FK_Reservations_Room]
            FOREIGN KEY ([room_id]) REFERENCES [dbo].[rooms]([room_id])
    );
END;
GO

-- Sample data (only when the tables are empty)
IF NOT EXISTS (SELECT 1 FROM [dbo].[rooms])
BEGIN
    INSERT INTO [dbo].[rooms] ([room_number], [room_type], [price_per_night], [capacity], [floor], [status]) VALUES
    (N'A-101', N'SINGLE', 500000,  1, 1, N'AVAILABLE'),
    (N'A-102', N'DOUBLE', 850000,  2, 1, N'AVAILABLE'),
    (N'B-201', N'SUITE',  1500000, 4, 2, N'AVAILABLE'),
    (N'B-202', N'DELUXE', 2500000, 6, 2, N'OCCUPIED'),
    (N'C-301', N'DOUBLE', 900000,  2, 3, N'MAINTENANCE');
END;
GO

IF NOT EXISTS (SELECT 1 FROM [dbo].[reservations])
BEGIN
    INSERT INTO [dbo].[reservations] ([guest_name], [guest_email], [guest_phone], [check_in_date], [check_out_date],
                                      [number_of_guests], [total_amount], [status], [room_id]) VALUES
    (N'Nguyen Van An',  N'an@mail.com',   N'0901111111', '2026-11-01', '2026-11-03', 2, 1700000, N'CONFIRMED',   2),
    (N'Tran Thi Binh',  N'binh@mail.com', N'0902222222', '2026-10-20', '2026-10-25', 5, 12500000, N'CHECKED_IN', 4),
    (N'Le Van Cuong',   N'cuong@mail.com',N'0903333333', '2026-10-01', '2026-10-02', 1, 500000,  N'CHECKED_OUT', 1);
END;
GO
