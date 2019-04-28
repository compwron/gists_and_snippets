# frozen_string_literal: true

class TicTacToe
  class SpotTaken < StandardError
  end

  SIZE = 3
  DEFAULT = '-'

  def initialize
    @board = Array.new(SIZE) { Array.new(SIZE) { |_| DEFAULT } }
  end

  def add_token(x, y, piece)
    raise SpotTaken if spot_taken?(x, y)

    @board[x][y] = piece
  end

  def print_board
    @board.map { |i| i.join('|') }
  end

  def is_full?
    @board.all? { |i| i.all? { |j| j != DEFAULT } }
  end

  def spot_taken?(x, y)
    @board[x][y] != DEFAULT
  end

  def ai_move
    @board.each_with_index do |x, x_idx|
      x.each_with_index do |y, y_idx|
        if y == DEFAULT
          @board[x_idx][y_idx] = 'O'
          return
        end
      end
    end
    raise 'Could not find a move'
  end

  def game_won?
    %w[X O].each do |player_piece|
      @board.map do |x|
        all_player_piece = x.all? do |y|
          y == player_piece
        end
        return true if all_player_piece
      end

      y_wins = [
        [[0, 0], [1, 0], [2, 0]],
        [[0, 1], [1, 1], [2, 1]],
        [[0, 2], [1, 2], [2, 2]]
      ]
      y_wins.each do |win_data|
        moves = win_data.map do |x, y|
          @board[x][y]
        end
        return true if moves.first != DEFAULT && moves.uniq.count == 1
      end

      diagonal_wins = [
        [[0, 0], [1, 1], [2, 2]],
        [[0, 2], [1, 1], [2, 0]]
      ]
      diagonal_wins.each do |win_data|
        moves = win_data.map do |x, y|
          @board[x][y]
        end
        return true if moves.first != DEFAULT && moves.uniq.count == 1
      end
    end
    false
  end
end

board = TicTacToe.new
if board.game_won?
  puts board.print_board
  raise 'oops'
end

board = TicTacToe.new
board.add_token(0, 0, 'X')
board.add_token(0, 1, 'X')
board.add_token(0, 2, 'X')
unless board.game_won?
  puts board.print_board
  raise 'oops'
end

board = TicTacToe.new
board.add_token(1, 0, 'X')
board.add_token(1, 1, 'X')
board.add_token(1, 2, 'X')
unless board.game_won?
  puts board.print_board
  raise 'oops'
end

board = TicTacToe.new
board.add_token(2, 0, 'X')
board.add_token(2, 1, 'X')
board.add_token(2, 2, 'X')
unless board.game_won?
  puts board.print_board
  raise 'oops'
end

board = TicTacToe.new
board.add_token(0, 0, 'X')
board.add_token(1, 0, 'X')
board.add_token(2, 0, 'X')
unless board.game_won?
  puts board.print_board
  raise 'oops'
end

board = TicTacToe.new
board.add_token(0, 0, 'X')
board.add_token(1, 1, 'X')
board.add_token(2, 2, 'X')
unless board.game_won?
  puts board.print_board
  raise 'oops'
end

board = TicTacToe.new
board.add_token(0, 2, 'X')
board.add_token(1, 1, 'X')
board.add_token(2, 0, 'X')
unless board.game_won?
  puts board.print_board
  raise 'oops'
end

board = TicTacToe.new
until board.is_full?
  puts 'Input move'
  user_data = gets
  split_data = user_data.strip.split(' ').map(&:to_i)
  begin
    board.add_token(*split_data, 'X')
    puts 'Your move was:'
    puts board.print_board
    if board.game_won?
      puts 'You win!'
      exit
    end

    board.ai_move
    puts 'The AI move was:'
    puts board.print_board
    if board.game_won?
      puts 'The AI won!'
      exit
    end
  rescue TicTacToe::SpotTaken
    puts 'Spot taken; try again'
  end
end

# til done for today
# detect solved

# done later
# ai optimal algo

# someday TODO
# make newest move red
